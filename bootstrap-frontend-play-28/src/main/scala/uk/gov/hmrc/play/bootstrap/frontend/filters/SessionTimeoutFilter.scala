/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.play.bootstrap.frontend.filters

import akka.stream.Materializer
import play.api.Configuration
import play.api.mvc.request.{Cell, RequestAttrKey}
import play.api.mvc.{Filter, RequestHeader, Result, Session}
import uk.gov.hmrc.http.SessionKeys._
import uk.gov.hmrc.http.{HeaderNames, SessionKeys}

import java.time.{Duration, Instant}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.Option.option2Iterable
import scala.concurrent.{ExecutionContext, Future}

case class SessionTimeoutFilterConfig(
  timeoutDuration      : Duration,
  additionalSessionKeys: Set[String] = Set.empty,
  onlyWipeAuthToken    : Boolean     = false
)

object SessionTimeoutFilterConfig {

  def fromConfig(configuration: Configuration): SessionTimeoutFilterConfig = {

    val timeoutDuration = Duration.ofSeconds(
      configuration
        .getOptional[Long]("session.timeoutSeconds")
        .getOrElse(configuration.get[scala.concurrent.duration.Duration]("session.timeout").toSeconds)
    )

    val wipeIdleSession = configuration
      .get[Boolean]("session.wipeIdleSession")

    val additionalSessionKeysToKeep = configuration
      .get[Seq[String]]("session.additionalSessionKeysToKeep")
      .toSet

    SessionTimeoutFilterConfig(
      timeoutDuration       = timeoutDuration,
      additionalSessionKeys = additionalSessionKeysToKeep,
      onlyWipeAuthToken     = !wipeIdleSession
    )
  }
}

/**
  * Filter that manipulates session data if 'ts' session field is older than configured timeout.
  *
  * If the 'ts' has expired, we wipe the session, add a new SessionId and update the 'ts'.
  * If the 'ts' doesn't exist, or is invalid, we just wipe the authToken.
  *
  * This filter clears data on the incoming request, so that the controller does not receive any session information.
  * It also changes the SET-COOKIE header for the outgoing request, so that the browser knows the session has expired.
  *
  * A white-list of session values are omitted from this process.
  *
  * @param config          an instance of `SessionTimeoutFilterConfig` representing the various configurable aspects
  *                        of this class
  * @param mat             a `Materializer` instance for Play! to use when dealing with the underlying Akka streams
  */
@Singleton
class SessionTimeoutFilter(
  config: SessionTimeoutFilterConfig,
  mkSessionId: () => String = () => s"sessionId-${UUID.randomUUID()}",
  clock: () => Instant = () => Instant.now()
)(implicit
  ec: ExecutionContext,
  override val mat: Materializer
) extends Filter {
  @Inject
  def this(mat: Materializer, config: SessionTimeoutFilterConfig, ec: ExecutionContext) =
    this(config)(ec, mat)

  val authRelatedKeys = Seq(authToken)

  private def wipeFromSession(session: Session, keys: Seq[String]): Session = keys.foldLeft(session)((s, k) => s - k)

  override def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {

    val updateTimestamp: (Result) => Result =
      result => result.addingToSession(lastRequestTimestamp -> clock().toEpochMilli.toString)(rh)

    val wipeAllFromSessionCookie: (Result) => Result =
      result => result.withSession(preservedSessionData(result.session(rh)): _*)

    val wipeAuthRelatedKeysFromSessionCookie: (Result) => Result =
      result => result.withSession(wipeFromSession(result.session(rh), authRelatedKeys))

    val timestamp = rh.session.get(lastRequestTimestamp)

    val sessionId: String = mkSessionId()

    def addSessionIdKeyAndHeader(requestHeader: RequestHeader): RequestHeader =
      requestHeader
        .withHeaders(requestHeader.headers
                      .remove(HeaderNames.xSessionId)
                      .add   (HeaderNames.xSessionId -> sessionId)
                    )
        .addAttr(RequestAttrKey.Session, Cell(requestHeader.session + (SessionKeys.sessionId, sessionId)))

    val withSessionId: Result => Result =
      result => {
        val sessionKeyPair = SessionKeys.sessionId -> sessionId
        result.withSession(result.newSession.getOrElse(Session() + sessionKeyPair) + sessionKeyPair)
      }

    (timestamp.flatMap(timestampToInstant) match {
      case Some(ts) if hasExpired(ts) && config.onlyWipeAuthToken =>
        f(addSessionIdKeyAndHeader(wipeAuthRelatedKeys(rh)))
          .map(wipeAuthRelatedKeysFromSessionCookie)
          .map(withSessionId)
      case Some(ts) if hasExpired(ts) =>
        f(addSessionIdKeyAndHeader(wipeSession(rh)))
          .map(wipeAllFromSessionCookie)
          .map(withSessionId)
      case _ =>
        f(rh)
    }).map(updateTimestamp)
  }

  private def timestampToInstant(timestampMs: String): Option[Instant] =
    try {
      Some(Instant.ofEpochMilli(timestampMs.toLong))
    } catch {
      case e: NumberFormatException => None
    }

  private def hasExpired(timestamp: Instant): Boolean = {
    val timeOfExpiry = timestamp.plus(config.timeoutDuration)
    clock().isAfter(timeOfExpiry)
  }

  private def wipeSession(requestHeader: RequestHeader): RequestHeader = {
    val sessionMap: Map[String, String] = preservedSessionData(requestHeader.session).toMap
    requestWithUpdatedSession(requestHeader, new Session(sessionMap))
  }

  private def wipeAuthRelatedKeys(requestHeader: RequestHeader): RequestHeader =
    requestWithUpdatedSession(requestHeader, wipeFromSession(requestHeader.session, authRelatedKeys))

  private def requestWithUpdatedSession(requestHeader: RequestHeader, session: Session): RequestHeader =
    requestHeader.addAttr(
      key   = RequestAttrKey.Session,
      value = Cell(session)
    )



  private def preservedSessionData(session: Session): Seq[(String, String)] =
    for {
      key   <- (SessionTimeoutFilter.allowlistedSessionKeys ++ config.additionalSessionKeys).toSeq
      value <- session.get(key)
    } yield key -> value

}


object SessionTimeoutFilter {

  private[filters] val allowlistedSessionKeys: Set[String] = Set(
    lastRequestTimestamp, // the timestamp that this filter manages
    redirect, // a redirect used by some authentication provider journeys
    loginOrigin, // the name of a service that initiated a login
    "Csrf-Token", // the Play default name for a header that contains the CsrfToken value (here only in case it is being misused in tests)
    "csrfToken" // the Play default name for the CsrfToken value within the Play Session)
  )
}
