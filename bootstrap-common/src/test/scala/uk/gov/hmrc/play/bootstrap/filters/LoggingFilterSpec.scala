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

package uk.gov.hmrc.play.bootstrap.filters

import java.util.{Date, TimeZone}

import akka.stream.Materializer
import org.mockito.scalatest.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, OptionValues}
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.slf4j.Logger
import org.slf4j.helpers.NOPLogger
import play.api.{LoggerLike, MarkerContext}
import play.api.mvc.{RequestHeader, Results}
import play.api.routing.HandlerDef
import play.api.routing.Router.Attrs
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.reflectiveCalls

class LoggingFilterSpec
  extends AnyWordSpecLike
     with MockitoSugar
     with Matchers
     with OptionValues
     with FutureAwaits
     with DefaultAwaitTimeout
     with Eventually
     with BeforeAndAfterAll {

  var defaultJvmTimezone = TimeZone.getDefault

  override def beforeAll(): Unit =
    super.beforeAll()

  "the LoggingFilter should" should {
    "log when a request's path matches a controller which is configured to log" in new Setup {
      val logger        = createLogger()
      val loggingFilter = new TestLoggingFilter(logger, controllerNeedsLogging = true)

      val result = await(loggingFilter(testReqToResp)(requestWithHandlerInAttrs))
      result.header.status shouldBe 204

      assert(logger.loggingHappened)
    }

    "log when info about matched controller is not present" in new Setup {
      val logger        = createLogger()
      val loggingFilter = new TestLoggingFilter(logger, controllerNeedsLogging = true)

      val reqWithoutHandlerDef = FakeRequest() // missing HandlerDef attr, can it happen in reality?
      await(loggingFilter(testReqToResp)(reqWithoutHandlerDef))

      assert(logger.loggingHappened)
    }

    "not log when controller is not configured to log" in new Setup {
      val logger        = createLogger()
      val loggingFilter = new TestLoggingFilter(logger, controllerNeedsLogging = false)

      await(loggingFilter(testReqToResp)(requestWithHandlerInAttrs))

      assert(!logger.loggingHappened)
    }

    "log at info level with expected format and data" in new Setup {
      val logger = createLogger()

      val timestamp                  = new Date()
      val expectedRequestStartMillis = timestamp.getTime

      val expectedRequestDurationInMillis = 5
      val now                             = mock[() => Long]
      when(now.apply())
        .thenReturn(expectedRequestStartMillis)
        .andThen(expectedRequestStartMillis + expectedRequestDurationInMillis)

      val loggingFilter = new TestLoggingFilter(logger, controllerNeedsLogging = true, now)

      val result = await(loggingFilter(testReqToResp)(requestWithHandlerInAttrs))

      logger.loggedMessage.get shouldEqual
        s"""GET / ${result.header.status} ${expectedRequestDurationInMillis}ms"""
    }

    "log elapsed time and exception and return the failed future unchanged" in new Setup {
      val logger = createLogger()

      val timestamp                  = new Date()
      val expectedRequestStartMillis = timestamp.getTime

      val expectedRequestDurationInMillis = 5
      val now                             = mock[() => Long]
      when(now.apply())
        .thenReturn(expectedRequestStartMillis)
        .andThen(expectedRequestStartMillis + expectedRequestDurationInMillis)

      val loggingFilter = new TestLoggingFilter(logger, controllerNeedsLogging = true, now)
      val ex            = new Exception("test-exception")

      intercept[Exception](await(loggingFilter(_ => Future.failed(ex))(requestWithHandlerInAttrs))) shouldBe ex

      logger.loggedMessage.get shouldEqual
        s"""GET / $ex ${expectedRequestDurationInMillis}ms"""
    }
  }

  private def createLogger() = new LoggerLike {
    var loggedMessage: Option[String] = None
    override val logger: Logger       = NOPLogger.NOP_LOGGER

    override def info(s: => String)(implicit mc: MarkerContext): Unit =
      loggedMessage = Some(s)

    lazy val loggingHappened: Boolean = loggedMessage.isDefined
  }

  trait Setup {
    val testReqToResp =
      (_: RequestHeader) => Future.successful(Results.NoContent)

    val handlerDef = mock[HandlerDef](withSettings.lenient)
    when(handlerDef.controller)
      .thenReturn("controller-name")

    val requestWithHandlerInAttrs = FakeRequest().addAttr(Attrs.HandlerDef, handlerDef)
  }

  class TestLoggingFilter(
    loggerIn              : LoggerLike,
    controllerNeedsLogging: Boolean,
    currentTime           : () => Long = () => System.currentTimeMillis()
  ) extends LoggingFilter {

    override implicit val mat: Materializer = null
    override implicit val ec                = ExecutionContext.fromExecutorService(null)
    override def logger: LoggerLike         = loggerIn
    override val now: () => Long            = () => currentTime()
    override def controllerNeedsLogging(controllerName: String): Boolean =
      controllerNeedsLogging
  }
}
