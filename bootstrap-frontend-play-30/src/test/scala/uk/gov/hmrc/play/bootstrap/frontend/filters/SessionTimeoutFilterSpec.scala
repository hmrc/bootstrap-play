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

import com.typesafe.config.ConfigFactory
import org.apache.pekko.stream.Materializer
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.http.{DefaultHttpFilters, HttpFilters}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Results.Ok
import play.api.mvc._
import play.api.routing.Router
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Configuration}
import uk.gov.hmrc.http.HeaderNames.xSessionId
import uk.gov.hmrc.http.SessionKeys.{authToken, lastRequestTimestamp, loginOrigin, sessionId => sessionIdKey}

import java.time.temporal.ChronoUnit
import java.time.{Duration, Instant, LocalDateTime, ZoneOffset}
import javax.inject.Inject
import scala.concurrent.ExecutionContext

object SessionTimeoutFilterSpec {
  val now: () => Instant = () => LocalDateTime.of(2017, 1, 12, 14, 56).toInstant(ZoneOffset.UTC)

  val mkSessionId: () => String = () => "some-unique-key-for-a-sessionId"

  class Filters @Inject()(timeoutFilter: SessionTimeoutFilter) extends DefaultHttpFilters(timeoutFilter)

  class StaticDateSessionTimeoutFilter @Inject()(
    config: SessionTimeoutFilterConfig
  )(implicit
    ec: ExecutionContext,
    mat: Materializer
  ) extends SessionTimeoutFilter(config, mkSessionId, now)(ec, mat)
}

class SessionTimeoutFilterSpec
  extends AnyWordSpecLike
     with Matchers
     with ScalaFutures
     with OptionValues
     with MockitoSugar {

  import SessionTimeoutFilterSpec._

  private val Action = stubControllerComponents().actionBuilder

  val builder: GuiceApplicationBuilder = {
    import play.api.routing.sird._
    new GuiceApplicationBuilder()
      .router(
        Router.from {
          case GET(p"/test") =>
            Action { request =>
              buildResult(request)
            }
          case GET(p"/testWithSessionFromService") =>
            Action { request =>
              buildResult(request).withSession( "allowlisted" -> "some-preserved-value")
            }
        }
      )
      .overrides(
        bind[SessionTimeoutFilter].to[StaticDateSessionTimeoutFilter],
        bind[HttpFilters].to[Filters]
      )
  }

  private def buildResult(request: Request[AnyContent]) = {
    Ok(
      Json.obj(
        "session" -> request.session.data,
        "headers" -> request.headers.toSimpleMap,
        "cookies" -> request.cookies.toSeq
          .map { cookie =>
            cookie.name -> cookie.value
          }
          .toMap[String, String]
      ))
  }

  "SessionTimeoutFilter" should {
    val timestamp = now().minus(5, ChronoUnit.MINUTES).toEpochMilli.toString

    val config = SessionTimeoutFilterConfig(
      timeoutDuration       = Duration.of(1, ChronoUnit.MINUTES),
      additionalSessionKeys = Set("allowlisted")
    )

    def app(config: SessionTimeoutFilterConfig = config): Application = {
      import play.api.inject._
      builder
        .overrides(
          bind[SessionTimeoutFilterConfig].toInstance(config)
        )
        .build()
    }

    "strip non-allowlist session variables from request if timestamp is old" in {
      running(app()) {
        val result = route(
          app(),
          FakeRequest(GET, "/test")
           .withSession(
            lastRequestTimestamp -> timestamp,
            authToken            -> "a-token",
            "allowlisted"        -> "allowlisted",
            sessionIdKey         -> "old-session-id"
          )
          .withHeaders(
            xSessionId -> "old-session-id")
        ).value

        val rhSession = (contentAsJson(result) \ "session").as[Map[String, String]]
        val rhHeader = (contentAsJson(result) \ "headers").as[Map[String,String]]

        rhSession                                  should onlyContainAllowlistedKeys(Set("allowlisted", sessionIdKey))
        rhSession.get(lastRequestTimestamp).value  shouldEqual timestamp
        rhSession.get("allowlisted").value         shouldEqual "allowlisted"
        rhSession.get(sessionIdKey).value          shouldEqual mkSessionId()
        rhHeader.get(xSessionId).value             shouldEqual mkSessionId()
      }
    }

    "strip non-allowlist session variables from result if timestamp is old" in {
      running(app()) {
        val result = route(
          app(),
          FakeRequest(GET, "/test")
            .withSession(
             lastRequestTimestamp -> timestamp,
             loginOrigin          -> "gg",
             authToken            -> "a-token",
             "allowlisted"        -> "allowlisted",
             sessionIdKey         -> "old-session-id"
            )
            .withHeaders(
              xSessionId -> "some-x-session-id")
        ).value

        val rhSession = (contentAsJson(result) \ "session").as[Map[String, String]]
        val rhHeaders = (contentAsJson(result) \ "headers").as[Map[String, String]]

        rhSession                                   should onlyContainAllowlistedKeys(Set("allowlisted", sessionIdKey))
        rhSession.get(loginOrigin)                  shouldBe Some("gg")
        rhSession.get("allowlisted")                shouldBe Some("allowlisted")
        rhSession.get(sessionIdKey).value           shouldEqual mkSessionId()
        rhHeaders.get(xSessionId).value             shouldEqual mkSessionId()
      }
    }


    "preserve values in result session when one exists with values" in {
      running(app()) {
        val result = route(
          app(),
          FakeRequest(GET, "/testWithSessionFromService")
            .withSession(
              lastRequestTimestamp -> timestamp,
              authToken -> "a-token",
              "allowlisted" -> "allowlisted",
              sessionIdKey -> "old-session-id"
            )
            .withHeaders(
              xSessionId -> "old-session-id")
        ).value

        val rhSession = (contentAsJson(result) \ "session").as[Map[String, String]]
        val rhHeader = (contentAsJson(result) \ "headers").as[Map[String, String]]

        rhSession should onlyContainAllowlistedKeys(Set("allowlisted", sessionIdKey))
        rhSession.get(lastRequestTimestamp).value shouldEqual timestamp
        rhSession.get("allowlisted").value shouldEqual "allowlisted"
        rhSession.get(sessionIdKey).value shouldEqual mkSessionId()
        rhHeader.get(xSessionId).value shouldEqual mkSessionId()

        session(result).get("allowlisted") shouldBe Some("some-preserved-value")
        session(result).get(sessionIdKey) shouldBe Some(mkSessionId())

      }
    }
    "pass through all session values if timestamp is recent" in {
      val timestamp = now().minusSeconds(5).toEpochMilli.toString

      running(app()) {
        val result = route(
          app(),
          FakeRequest(GET, "/test").withSession(
            lastRequestTimestamp -> timestamp,
            authToken            -> "a-token",
            "custom"             -> "custom",
            sessionIdKey         -> "some-session-id"
          )
          .withHeaders(
            xSessionId -> "some-session-id")
        ).value

        val rhSession = (contentAsJson(result) \ "session").as[Map[String, String]]
        val rhHeaders = (contentAsJson(result) \ "headers").as[Map[String, String]]

        rhSession               shouldNot onlyContainAllowlistedKeys(Set("allowlisted"))
        rhSession.get("custom") shouldBe Some("custom")
        rhHeaders.get(xSessionId).value   shouldEqual "some-session-id"

        session(result).get("custom")     shouldBe Some("custom")
        session(result).get(sessionIdKey) shouldBe Some("some-session-id")
      }
    }

    "create timestamp if it's missing" in {
      running(app()) {
        val result = route(
          app(),
          FakeRequest(GET, "/test").withSession(
            authToken    -> "a-token",
            "custom"     -> "custom",
            sessionIdKey -> "some-session-id"
          )
        ).value

        val rhSession = (contentAsJson(result) \ "session").as[Map[String, String]]

        rhSession.get(authToken).value      shouldEqual "a-token"
        rhSession.get("custom").value       shouldEqual "custom"
        rhSession.get(sessionIdKey).value   shouldEqual "some-session-id"
        rhSession.get(lastRequestTimestamp) shouldBe None

        session(result).get(lastRequestTimestamp) shouldBe Some(now().toEpochMilli.toString)
      }
    }

    "strip only auth-related keys if timestamp is old, and onlyWipeAuthToken == true" in {
      val altConfig    = config.copy(onlyWipeAuthToken = true)
      val oldTimestamp = now().minus(5, ChronoUnit.MINUTES).toEpochMilli.toString

      running(app(altConfig)) {

        val result = route(
          app(altConfig),
          FakeRequest(GET, "/test").withSession(
            lastRequestTimestamp -> oldTimestamp,
            authToken            -> "a-token",
            "custom"             -> "custom",
            "allowlisted"        -> "allowlisted",
            sessionIdKey         -> "some-session-id"
          )
        ).value

        val rhSession = (contentAsJson(result) \ "session").as[Map[String, String]]
        val rhHeaders = (contentAsJson(result) \ "headers").as[Map[String, String]]

        rhSession.get("custom").value   shouldEqual "custom"
        rhSession.get(authToken)        shouldNot be(defined)
        rhHeaders.get(xSessionId).value shouldEqual mkSessionId()

        session(result).get("custom").value     shouldEqual "custom"
        session(result).get(authToken)          shouldNot be(defined)
        session(result).get(sessionIdKey).value shouldEqual mkSessionId()
      }
    }

    "update old timestamp with current time" in {
      running(app()) {
        val result = route(
          app(),
          FakeRequest(GET, "/test").withSession(
            lastRequestTimestamp -> now().minus(1, ChronoUnit.DAYS).toEpochMilli.toString
          )
        ).value

        session(result).get(lastRequestTimestamp).value shouldEqual now().toEpochMilli.toString
      }
    }

    "update recent timestamp with current time" in {
      running(app()) {
        val result = route(
          app(),
          FakeRequest(GET, "/test").withSession(
            lastRequestTimestamp -> now().minusSeconds(1).toEpochMilli.toString
          )
        ).value

        session(result).get(lastRequestTimestamp).value shouldEqual now().toEpochMilli.toString
        session(result).get(sessionIdKey)               shouldNot be(defined)
      }
    }

    "treat an invalid timestamp as a missing timestamp" in {
      running(app()) {
        val result = route(
          app(),
          FakeRequest(GET, "/test").withSession(
            lastRequestTimestamp -> "invalid-format",
            authToken            -> "a-token",
            loginOrigin          -> "gg",
            "custom"             -> "custom",
            sessionIdKey         -> "some-session-id"
          )
        ).value

        session(result).get(authToken).value            shouldEqual "a-token"
        session(result).get(loginOrigin).value          shouldEqual "gg"
        session(result).get("custom").value             shouldEqual "custom"
        session(result).get(lastRequestTimestamp).value shouldEqual now().toEpochMilli.toString
        session(result).get(sessionIdKey).value         shouldEqual "some-session-id"
      }
    }

    "ensure non-session cookies are passed through to the action untouched" in {
      val timestamp = now().minus(5, ChronoUnit.MINUTES).toEpochMilli.toString

      running(app()) {

        val request = FakeRequest(GET, "/test")
          .withCookies(Cookie("aTestName", "aTestValue"))
          .withSession(
            lastRequestTimestamp -> timestamp,
            authToken            -> "a-token"
          )

        val result    = route(app(), request).value
        val rhCookies = (contentAsJson(result) \ "cookies").as[Map[String, String]]

        rhCookies      should contain("aTestName" -> "aTestValue")
        status(result) shouldEqual OK
      }
    }
  }

  "SessionTimeoutFilterConfig.fromConfig" should {
    "return defaults when there is no config" in {
      val config = Configuration(ConfigFactory.load("frontend.test.conf"))
      val result = SessionTimeoutFilterConfig.fromConfig(config)
      result.additionalSessionKeys should be (empty)
      result.onlyWipeAuthToken     shouldBe false
      result.timeoutDuration       shouldEqual Duration.of(15, ChronoUnit.MINUTES)
    }

    "return defaults when config is set to the defaults" in {
      val config = Configuration(
        "session.timeoutSeconds"              -> 900,
        "session.wipeIdleSession"             -> true,
        "session.additionalSessionKeysToKeep" -> Set.empty
      )
      val result = SessionTimeoutFilterConfig.fromConfig(config)
      result.additionalSessionKeys should be (empty)
      result.onlyWipeAuthToken     shouldBe false
      result.timeoutDuration       shouldEqual Duration.of(15, ChronoUnit.MINUTES)
    }

    "return custom settings" in {
      val config = Configuration(
        "session.timeoutSeconds"              -> 300,
        "session.wipeIdleSession"             -> false,
        "session.additionalSessionKeysToKeep" -> Set("foo")
      )
      val result = SessionTimeoutFilterConfig.fromConfig(config)
      result.additionalSessionKeys should contain("foo")
      result.onlyWipeAuthToken     shouldBe true
      result.timeoutDuration       shouldEqual Duration.of(5, ChronoUnit.MINUTES)
    }
  }

  private def onlyContainAllowlistedKeys(additionalSessionKeysToKeep: Set[String]) =
    new Matcher[Map[String, String]] {
      override def apply(data: Map[String, String]): MatchResult =
        MatchResult(
          (data.keySet -- SessionTimeoutFilter.allowlistedSessionKeys -- additionalSessionKeysToKeep).isEmpty,
          s"Session keys ${data.keySet} did not contain only allowlisted keys: ${SessionTimeoutFilter.allowlistedSessionKeys}",
          s"Session keys ${data.keySet} contained only allowlisted keys: ${SessionTimeoutFilter.allowlistedSessionKeys}"
        )
    }
}
