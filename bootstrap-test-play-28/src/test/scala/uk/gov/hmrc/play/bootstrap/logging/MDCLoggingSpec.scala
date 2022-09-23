/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.play.bootstrap.logging

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.slf4j.MDC
import play.api.{Configuration, Logger}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{Json, Writes}
import play.api.mvc.Results
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderNames => HMRCHeaderNames, SessionKeys}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, Promise}

abstract class MDCLoggingSpec
  extends AnyWordSpec
     with Matchers
     with ScalaFutures
     with OptionValues
     with BeforeAndAfterEach {

  override def beforeEach(): Unit =
    MDC.clear()

  override def afterEach(): Unit =
    MDC.clear()

  def anApplicationWithMDCLogging(configFile: String, isFrontend: Boolean): Unit = {

    val config = Configuration(ConfigFactory.load(configFile))

    lazy val router = {
      import play.api.routing._
      import play.api.routing.sird._

      val Action = stubControllerComponents().actionBuilder

      Router.from {
        case GET(p"/") =>
          Action {
            Logger(getClass).warn("bar")
            Results.Ok(
              Json.toJson(
                Option(MDC.getCopyOfContextMap)
                  .map(_.asScala)
                  .getOrElse(Map.empty[String, String])
              )(Writes.genericMapWrites)
            )
          }
      }
    }

    "injected dispatchers should be ready to use without calling prepare" in {
      lazy val app = new GuiceApplicationBuilder()
        .configure(config)
        .build()

      running(app) {
        val dispatcher = app.injector.instanceOf[ActorSystem].dispatcher

        val promise = Promise[Map[String, String]]()

        MDC.put("foo", "bar")

        dispatcher.execute(() => {
          val data =
            Option(MDC.getCopyOfContextMap)
              .fold(Map.empty[String, String])(_.asScala.toMap)

           promise.success(data)
        })

        promise.future.futureValue must contain ("foo" -> "bar")
      }
    }

    "must pass MDC information between thread contexts" in {
      lazy val app = new GuiceApplicationBuilder()
        .configure(config)
        .build()

      running(app) {
        implicit val ec: ExecutionContext =
          app.injector.instanceOf[ExecutionContext]

        MDC.put("foo", "bar")

        val future = Future {
          Option(MDC.get("foo"))
        }

        whenReady(future) {
          _.value mustEqual "bar"
        }
      }
    }

    "must add all request information to the MDC" in {
      lazy val app = new GuiceApplicationBuilder()
        .configure(config)
        .router(router)
        .build()

      running(app) {
        val request =
          if (isFrontend)
            FakeRequest(GET, "/")
              .withHeaders(
                HMRCHeaderNames.xRequestId    -> "some request id",
                HMRCHeaderNames.xForwardedFor -> "some forwarded for"
              ).withSession(SessionKeys.sessionId -> "some session id")
          else
            FakeRequest(GET, "/")
              .withHeaders(
                HMRCHeaderNames.xSessionId    -> "some session id",
                HMRCHeaderNames.xRequestId    -> "some request id",
                HMRCHeaderNames.xForwardedFor -> "some forwarded for"
              )

        val result = route(app, request).value

        status(result) mustBe OK

        val mdc = contentAsJson(result).as[Map[String, String]]

        mdc must contain.only(
          HMRCHeaderNames.xSessionId    -> "some session id",
          HMRCHeaderNames.xRequestId    -> "some request id",
          HMRCHeaderNames.xForwardedFor -> "some forwarded for"
        )
      }
    }
  }
}
