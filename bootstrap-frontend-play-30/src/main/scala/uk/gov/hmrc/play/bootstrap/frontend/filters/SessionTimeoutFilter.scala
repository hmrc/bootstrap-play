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

import org.apache.pekko.stream.Materializer
import play.api.Configuration
import play.api.mvc.{Filter, Headers, RequestHeader, Result, Session}
import play.api.mvc.request.{Cell, RequestAttrKey}
import uk.gov.hmrc.http.{HeaderNames, SessionKeys}

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{Duration, DurationLong}

case class SessionTimeoutFilterConfig(
  timeoutDuration      : Duration,
  additionalSessionKeys: Set[String] = Set.empty,
  onlyWipeAuthToken    : Boolean     = false
)

object SessionTimeoutFilterConfig {
  def fromConfig(configuration: Configuration): SessionTimeoutFilterConfig =
    SessionTimeoutFilterConfig(
      timeoutDuration       = configuration.getOptional[Long]("session.timeoutSeconds").map(_.seconds)
                                .getOrElse(configuration.get[Duration]("session.timeout")),
      additionalSessionKeys = configuration.get[Seq[String]]("session.additionalSessionKeysToKeep").toSet,
      onlyWipeAuthToken     = !configuration.get[Boolean]("session.wipeIdleSession")
    )
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
  config          : SessionTimeoutFilterConfig,
  mkSessionId     : () => String  = () => s"session-${UUID.randomUUID()}",
  clock           : () => Instant = () => Instant.now()
)(implicit
  ec              : ExecutionContext,
  override val mat: Materializer
) extends Filter {

  @Inject
  def this(
    mat   : Materializer,
    config: SessionTimeoutFilterConfig,
    ec    : ExecutionContext
  ) =
    this(config)(ec, mat)

  private def removeExpiredData(session: Session): Session =
    if (config.onlyWipeAuthToken)
      removeFromSession(session, SessionTimeoutFilter.authRelatedKeys)
    else
      removeAllExceptFromSession(session, SessionTimeoutFilter.allowlistedSessionKeys ++ config.additionalSessionKeys)

  override def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
    lazy val sessionId: String = mkSessionId()

    (timestamp(rh) match {
      case Some(ts) if hasExpired(ts) =>
        val updatedRequestHeader =
          transformHeadersInRequest(_.remove(HeaderNames.xSessionId).add(HeaderNames.xSessionId -> sessionId))(
            transformSessionInRequest(_ + (SessionKeys.sessionId, sessionId))(
              transformSessionInRequest(removeExpiredData)(
                rh
              )
            )
          )

        f(updatedRequestHeader)
          .map(transformSessionInResult(removeExpiredData)(updatedRequestHeader))
          .map(transformSessionInResult(_ + (SessionKeys.sessionId, sessionId))(updatedRequestHeader))

      case _ =>
        f(rh)
    }).map(updateTimestamp(rh))
  }

  private def timestamp(requestHeader: RequestHeader): Option[Instant] =
    requestHeader.session.get(SessionKeys.lastRequestTimestamp)
      .flatMap(timestampMs =>
        try
          Some(Instant.ofEpochMilli(timestampMs.toLong))
        catch {
          case e: NumberFormatException => None
        }
      )

  private def hasExpired(timestamp: Instant): Boolean =
    clock().isAfter(timestamp.plus(config.timeoutDuration.toSeconds, ChronoUnit.SECONDS))

  private def updateTimestamp(requestHeader: RequestHeader)(result: Result): Result =
    result.addingToSession(SessionKeys.lastRequestTimestamp -> clock().toEpochMilli.toString)(requestHeader)

  private def transformHeadersInRequest(transform: Headers => Headers)(requestHeader: RequestHeader): RequestHeader =
    requestHeader.withHeaders(transform(requestHeader.headers))

  private def transformSessionInRequest(transform: Session => Session)(requestHeader: RequestHeader): RequestHeader =
    requestHeader.addAttr(
      key   = RequestAttrKey.Session,
      value = Cell(transform(requestHeader.session))
    )

  private def transformSessionInResult(transform: Session => Session)(requestHeader: RequestHeader)(result: Result) =
    result.withSession(transform(result.session(requestHeader)))

  private def removeFromSession(session: Session, keys: Set[String]): Session =
    keys.foldLeft(session)((s, k) => s - k)

  private def removeAllExceptFromSession(session: Session, keys: Set[String]): Session =
    new Session(
      (for {
         key   <- keys
         value <- session.get(key)
       } yield key -> value
      ).toMap
    )
}


object SessionTimeoutFilter {

  private val authRelatedKeys: Set[String] = Set(
    SessionKeys.authToken
  )

  private[filters] val allowlistedSessionKeys: Set[String] = Set(
    SessionKeys.lastRequestTimestamp, // the timestamp that this filter manages
    SessionKeys.redirect,             // a redirect used by some authentication provider journeys
    SessionKeys.loginOrigin,          // the name of a service that initiated a login
    "Csrf-Token",                     // the Play default name for a header that contains the CsrfToken value (here only in case it is being misused in tests)
    "csrfToken"                       // the Play default name for the CsrfToken value within the Play Session)
  )
}
