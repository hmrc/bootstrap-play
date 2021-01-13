/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.play.bootstrap.backend.http

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.mockito.ArgumentMatchers.{any, eq => is}
import org.mockito.Mockito._
import org.scalatest.LoneElement
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.{Configuration, Logger, LoggerLike}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthorisationException
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.bootstrap.config.HttpAuditEvent

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class JsonErrorHandlerSpec
  extends AnyWordSpec
     with Matchers
     with ScalaFutures
     with MockitoSugar
     with LoneElement
     with Eventually {

  import ExecutionContext.Implicits.global

  "onServerError" should {

    "convert a NotFoundException to NotFound response and audit the error" in new Setup {
      val notFoundException = new NotFoundException("test")
      val createdDataEvent  = DataEvent("auditSource", "auditType")
      when(
        httpAuditEvent.dataEvent(
          eventType       = is("ResourceNotFound"),
          transactionName = is("Unexpected error"),
          request         = is(requestHeader),
          detail          = is(Map("transactionFailureReason" -> notFoundException.getMessage))
        )(any[HeaderCarrier]))
        .thenReturn(createdDataEvent)

      val result = jsonErrorHandler.onServerError(requestHeader, notFoundException)

      status(result)        shouldEqual NOT_FOUND
      contentAsJson(result) shouldEqual Json.obj("statusCode" -> NOT_FOUND, "message" -> "test")

      verify(auditConnector).sendEvent(is(createdDataEvent))(any[HeaderCarrier], any[ExecutionContext])
    }

    "convert an AuthorisationException to Unauthorized response and audit the error" in new Setup {
      val authorisationException = new AuthorisationException("reason") {}
      val createdDataEvent       = DataEvent("auditSource", "auditType")
      when(
        httpAuditEvent.dataEvent(
          eventType       = is("ClientError"),
          transactionName = is("Unexpected error"),
          request         = is(requestHeader),
          detail          = is(Map("transactionFailureReason" -> authorisationException.getMessage))
        )(any[HeaderCarrier]))
        .thenReturn(createdDataEvent)

      val result = jsonErrorHandler.onServerError(requestHeader, authorisationException)

      status(result) shouldEqual UNAUTHORIZED
      contentAsJson(result) shouldEqual Json
        .obj("statusCode" -> UNAUTHORIZED, "message" -> authorisationException.getMessage)

      verify(auditConnector).sendEvent(is(createdDataEvent))(any[HeaderCarrier], any[ExecutionContext])
    }

    "convert an Exception to InternalServerError and audit the error" in new Setup {
      val exception        = new Exception("any application exception")
      val createdDataEvent = DataEvent("auditSource", "auditType")
      when(
        httpAuditEvent.dataEvent(
          eventType       = is("ServerInternalError"),
          transactionName = is("Unexpected error"),
          request         = is(requestHeader),
          detail          = is(Map("transactionFailureReason" -> exception.getMessage))
        )(any[HeaderCarrier]))
        .thenReturn(createdDataEvent)

      val result = jsonErrorHandler.onServerError(requestHeader, exception)

      status(result) shouldEqual INTERNAL_SERVER_ERROR
      contentAsJson(result) shouldEqual Json
        .obj("statusCode" -> INTERNAL_SERVER_ERROR, "message" -> exception.getMessage)

      verify(auditConnector).sendEvent(is(createdDataEvent))(any[HeaderCarrier], any[ExecutionContext])
    }

    "convert a JsValidationException to InternalServerError and audit the error" in new Setup {
      val exception        = new JsValidationException(GET, uri, classOf[Int], "json deserialization error")
      val createdDataEvent = DataEvent("auditSource", "auditType")
      when(
        httpAuditEvent.dataEvent(
          eventType       = is("ServerValidationError"),
          transactionName = is("Unexpected error"),
          request         = is(requestHeader),
          detail          = is(Map("transactionFailureReason" -> exception.getMessage))
        )(any[HeaderCarrier]))
        .thenReturn(createdDataEvent)

      val result = jsonErrorHandler.onServerError(requestHeader, exception)

      status(result) shouldEqual INTERNAL_SERVER_ERROR
      contentAsJson(result) shouldEqual Json
        .obj("statusCode" -> INTERNAL_SERVER_ERROR, "message" -> exception.getMessage)

      verify(auditConnector).sendEvent(is(createdDataEvent))(any[HeaderCarrier], any[ExecutionContext])
    }

    "convert a HttpException to responseCode from the exception and audit the error" in new Setup {
      val responseCode     = randomErrorStatusCode()
      val exception        = new HttpException("error message", responseCode)
      val createdDataEvent = DataEvent("auditSource", "auditType")
      when(
        httpAuditEvent.dataEvent(
          eventType       = is("ServerInternalError"),
          transactionName = is("Unexpected error"),
          request         = is(requestHeader),
          detail          = is(Map("transactionFailureReason" -> exception.getMessage))
        )(any[HeaderCarrier]))
        .thenReturn(createdDataEvent)

      val result = jsonErrorHandler.onServerError(requestHeader, exception)

      status(result) shouldEqual responseCode
      contentAsJson(result) shouldEqual Json
        .obj("statusCode" -> responseCode, "message" -> exception.getMessage)

      verify(auditConnector).sendEvent(is(createdDataEvent))(any[HeaderCarrier], any[ExecutionContext])
    }

    "convert an UpstreamErrorResponse to reportAs from the exception and audit the error" in new Setup {
      val reportAs         = randomErrorStatusCode()
      val exception        = UpstreamErrorResponse("error message", randomErrorStatusCode(), reportAs)
      val createdDataEvent = DataEvent("auditSource", "auditType")
      when(
        httpAuditEvent.dataEvent(
          eventType       = is("ServerInternalError"),
          transactionName = is("Unexpected error"),
          request         = is(requestHeader),
          detail          = is(Map("transactionFailureReason" -> exception.getMessage))
        )(any[HeaderCarrier]))
        .thenReturn(createdDataEvent)

      val result = jsonErrorHandler.onServerError(requestHeader, exception)

      status(result) shouldEqual reportAs
      contentAsJson(result) shouldEqual Json
        .obj("statusCode" -> reportAs, "message" -> exception.getMessage)

      verify(auditConnector).sendEvent(is(createdDataEvent))(any[HeaderCarrier], any[ExecutionContext])
    }

    "log a warning for upstream code in the warning list" when {
      class WarningSetup(upstreamWarnStatuses: Seq[Int]) extends Setup {
        override val configuration: Configuration = Configuration(
          "appName" -> "myApp",
          "bootstrap.errorHandler.warnOnly.statusCodes" -> upstreamWarnStatuses
        )
      }

      def withCaptureOfLoggingFrom(loggerLike: LoggerLike)(body: (=> List[ILoggingEvent]) => Unit) {
        import ch.qos.logback.classic.{Logger => LogbackLogger}
        import scala.collection.JavaConverters._

        val logger = loggerLike.logger.asInstanceOf[LogbackLogger]
        val appender = new ListAppender[ILoggingEvent]()
        appender.setContext(logger.getLoggerContext)
        appender.start()
        logger.addAppender(appender)
        logger.setLevel(Level.ALL)
        logger.setAdditive(true)
        body(appender.list.asScala.toList)
      }

      "an UpstreamErrorResponse exception occurs" in new WarningSetup(Seq(500)) {
        withCaptureOfLoggingFrom(Logger(classOf[JsonErrorHandler])) { logEvents =>
          jsonErrorHandler.onServerError(requestHeader, UpstreamErrorResponse("any application exception", 500, 502)).futureValue

          eventually {
            val event = logEvents.loneElement
            event.getLevel   shouldBe Level.WARN
            event.getMessage shouldBe s"any application exception"
          }
        }
      }

      "a HttpException occurs" in new WarningSetup(Seq(400)) {
        withCaptureOfLoggingFrom(Logger(classOf[JsonErrorHandler])) { logEvents =>
          jsonErrorHandler.onServerError(requestHeader, new BadRequestException("any application exception")).futureValue

          eventually {
            val event = logEvents.loneElement
            event.getLevel   shouldBe Level.WARN
            event.getMessage shouldBe s"any application exception"
          }
        }
      }
    }

  }

  "onClientError" should {

    "audit an error and return json response for 400" in new Setup {
      val createdDataEvent = DataEvent("auditSource", "auditType")
      when(
        httpAuditEvent.dataEvent(
          eventType       = is("ServerValidationError"),
          transactionName = is("Request bad format exception"),
          request         = is(requestHeader),
          detail          = is(Map.empty)
        )(any[HeaderCarrier]))
        .thenReturn(createdDataEvent)

      val result = jsonErrorHandler.onClientError(requestHeader, BAD_REQUEST, "some message we want to override")

      status(result)        shouldEqual BAD_REQUEST
      contentAsJson(result) shouldEqual Json.obj("statusCode" -> BAD_REQUEST, "message" -> "bad request")

      verify(auditConnector).sendEvent(is(createdDataEvent))(any[HeaderCarrier], any[ExecutionContext])
    }

    "audit an error and return json response for 404 including requested path" in new Setup {
      val createdDataEvent = DataEvent("auditSource", "auditType")
      when(
        httpAuditEvent.dataEvent(
          eventType       = is("ResourceNotFound"),
          transactionName = is("Resource Endpoint Not Found"),
          request         = is(requestHeader),
          detail          = is(Map.empty)
        )(any[HeaderCarrier]))
        .thenReturn(createdDataEvent)

      val result = jsonErrorHandler.onClientError(requestHeader, NOT_FOUND, "some message we want to override")

      status(result) shouldEqual NOT_FOUND
      contentAsJson(result) shouldEqual Json
        .obj("statusCode" -> NOT_FOUND, "message" -> "URI not found", "requested" -> uri)

      verify(auditConnector).sendEvent(is(createdDataEvent))(any[HeaderCarrier], any[ExecutionContext])
    }

    "audit an error and return json response for 4xx except 404 and 400" in new Setup {
      (401 to 403) ++ (405 to 499) foreach { statusCode =>
        val createdDataEvent = DataEvent("auditSource", "auditType")
        when(
          httpAuditEvent.dataEvent(
            eventType       = is("ClientError"),
            transactionName = is(s"A client error occurred, status: $statusCode"),
            request         = is(requestHeader),
            detail          = is(Map.empty)
          )(any[HeaderCarrier]))
          .thenReturn(createdDataEvent)

        val errorMessage = "unauthorized"

        val result = jsonErrorHandler.onClientError(requestHeader, statusCode, errorMessage)

        status(result)        shouldEqual statusCode
        contentAsJson(result) shouldEqual Json.obj("statusCode" -> statusCode, "message" -> errorMessage)

        verify(auditConnector).sendEvent(is(createdDataEvent))(any[HeaderCarrier], any[ExecutionContext])
      }
    }
  }

  private trait Setup {
    val uri           = "some-uri"
    val requestHeader = FakeRequest(GET, uri)

    val auditConnector = mock[AuditConnector]
    when(auditConnector.sendEvent(any[DataEvent])(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.successful(Success))
    val httpAuditEvent = mock[HttpAuditEvent]

    val configuration    = Configuration(
      "appName" -> "myApp",
      "bootstrap.errorHandler.warnOnly.statusCodes" -> Seq.empty
      )
    lazy val jsonErrorHandler = new JsonErrorHandler(auditConnector, httpAuditEvent, configuration)

    def randomErrorStatusCode(): Int =
      400 + Random.nextInt(200)
  }
}
