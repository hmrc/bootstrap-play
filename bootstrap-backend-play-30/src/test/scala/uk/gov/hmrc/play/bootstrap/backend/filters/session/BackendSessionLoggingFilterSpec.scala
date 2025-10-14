/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.play.bootstrap.backend.filters.session

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.apache.pekko.stream.Materializer
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.{Configuration, Logger, LoggerLike, MarkerContext}
import play.api.mvc.Results._
import play.api.mvc._
import play.api.test.FakeRequest

import scala.concurrent.{ExecutionContext, Future}

class BackendSessionLoggingFilterSpec
  extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with ScalaFutures {

  "BackendSessionLoggingFilter" should {

    "log a warning when session data is present" in new Setup {
      val request = FakeRequest("GET", "/test").withSession("foo" -> "bar")
      withCaptureOfLoggingFrom(testLogger) { logEvents =>
        filterWithRealLogger.apply(_ => Future(Ok))(request).futureValue
        logEvents.exists(e => e.getLevel == Level.WARN && e.getFormattedMessage.contains("Session data detected")) shouldBe true
      }
    }

    "not log a warning when no session data is present" in new Setup {
      val request = FakeRequest("GET", "/test")
      filter.apply(_ => Future(Ok))(request).futureValue
      verifyNoInteractions(mockLogger)
    }
  }

  trait Setup {
    implicit val ec: ExecutionContext  = scala.concurrent.ExecutionContext.global
    implicit val mat: Materializer     = mock[Materializer]
    val mockLogger: Logger             = mock[Logger]
    val testLogger: Logger             = Logger("testLogger")

    val filter = new BackendSessionLoggingFilter {
      override protected implicit def ec: ExecutionContext = Setup.this.ec
      override def mat: Materializer                       = Setup.this.mat
      override protected val logger: Logger                = mockLogger
    }

    val filterWithRealLogger = new BackendSessionLoggingFilter {
      override protected implicit def ec: ExecutionContext = Setup.this.ec
      override def mat: Materializer                       = Setup.this.mat
      override protected val logger: Logger                = testLogger
    }

    def withCaptureOfLoggingFrom(loggerLike: LoggerLike)(body: (=> List[ILoggingEvent]) => Unit): Unit = {
      import ch.qos.logback.classic.{Logger => LogbackLogger}
      import scala.jdk.CollectionConverters._

      val logger = loggerLike.logger.asInstanceOf[LogbackLogger]
      val appender = new ListAppender[ILoggingEvent]()
      appender.setContext(logger.getLoggerContext)
      appender.start()
      logger.addAppender(appender)
      logger.setLevel(Level.WARN)
      logger.setAdditive(true)
      body(appender.list.asScala.toList)
    }
  }
}
