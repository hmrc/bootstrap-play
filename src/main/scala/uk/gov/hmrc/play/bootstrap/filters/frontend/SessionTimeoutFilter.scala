/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.play.bootstrap.filters.frontend

import akka.stream.Materializer
import javax.inject.Inject
import org.joda.time.{DateTime, DateTimeZone, Duration}
import play.api.Configuration
import play.api.mvc._
import play.api.mvc.request.{AssignedCell, RequestAttrKey}
import uk.gov.hmrc.http.SessionKeys._

import scala.concurrent.{ExecutionContext, Future}

case class SessionTimeoutFilterConfig(
  timeoutDuration: Duration,
  additionalSessionKeys: Set[String] = Set.empty,
  onlyWipeAuthToken: Boolean         = false
)

object SessionTimeoutFilterConfig {

  def fromConfig(configuration: Configuration): SessionTimeoutFilterConfig = {

    val defaultTimeout = Duration.standardMinutes(15)

    val timeoutDuration = configuration
      .getOptional[Long]("session.timeoutSeconds")
      .map(Duration.standardSeconds)
      .getOrElse(defaultTimeout)

    val wipeIdleSession = configuration
      .getOptional[Boolean]("session.wipeIdleSession")
      .getOrElse(true)

    val additionalSessionKeysToKeep = configuration
      .getOptional[Seq[String]]("session.additionalSessionKeysToKeep")
      .getOrElse(Seq.empty)
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
  * If the 'ts' has expired, we wipe the session, and update the 'ts'.
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
class SessionTimeoutFilter @Inject()(
  config: SessionTimeoutFilterConfig
)(
  implicit
  ec: ExecutionContext,
  override val mat: Materializer)
    extends Filter {

  def clock(): DateTime = DateTime.now(DateTimeZone.UTC)

  val authRelatedKeys = Seq(authToken, token, userId)

  private def wipeFromSession(session: Session, keys: Seq[String]): Session = keys.foldLeft(session)((s, k) => s - k)

  override def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {

    val updateTimestamp: (Result) => Result =
      result => result.addingToSession(lastRequestTimestamp -> clock().getMillis.toString)(rh)

    val wipeAllFromSessionCookie: (Result) => Result =
      result => result.withSession(preservedSessionData(result.session(rh)): _*)

    val wipeAuthRelatedKeysFromSessionCookie: (Result) => Result =
      result => result.withSession(wipeFromSession(result.session(rh), authRelatedKeys))

    val timestamp = rh.session.get(lastRequestTimestamp)

    (timestamp.flatMap(timestampToDatetime) match {
      case Some(ts) if hasExpired(ts) && config.onlyWipeAuthToken =>
        f(wipeAuthRelatedKeys(rh))
          .map(wipeAuthRelatedKeysFromSessionCookie)
      case Some(ts) if hasExpired(ts) =>
        f(wipeSession(rh))
          .map(wipeAllFromSessionCookie)
      case _ =>
        f(rh)
    }).map(updateTimestamp)
  }

  private def timestampToDatetime(timestamp: String): Option[DateTime] =
    try {
      Some(new DateTime(timestamp.toLong, DateTimeZone.UTC))
    } catch {
      case e: NumberFormatException => None
    }

  private def hasExpired(timestamp: DateTime): Boolean = {
    val timeOfExpiry = timestamp plus config.timeoutDuration
    clock() isAfter timeOfExpiry
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
      value = new AssignedCell(session)
    )

  private def preservedSessionData(session: Session): Seq[(String, String)] =
    for {
      key   <- (SessionTimeoutFilter.whitelistedSessionKeys ++ config.additionalSessionKeys).toSeq
      value <- session.get(key)
    } yield key -> value

}

object SessionTimeoutFilter {

  val whitelistedSessionKeys: Set[String] = Set(
    lastRequestTimestamp, // the timestamp that this filter manages
    redirect, // a redirect used by some authentication provider journeys
    loginOrigin, // the name of a service that initiated a login
    "Csrf-Token", // the Play default name for a header that contains the CsrfToken value (here only in case it is being misused in tests)
    "csrfToken", // the Play default name for the CsrfToken value within the Play Session)
    authProvider // a deprecated value that indicates what authentication provider was used for the session - may be used to handle default redirects on failed logins
  )
}
