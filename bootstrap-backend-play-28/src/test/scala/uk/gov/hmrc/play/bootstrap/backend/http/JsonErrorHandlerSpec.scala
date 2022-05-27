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

package uk.gov.hmrc.play.bootstrap.backend.http

import akka.actor.ActorSystem
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.mockito.scalatest.MockitoSugar
import org.scalatest.{EitherValues, LoneElement}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpec
import play.api.{Configuration, Logger, LoggerLike}
import play.api.http.{HttpErrorHandler, MimeTypes, HeaderNames => PlayHeaderNames}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.core.routing.RouteParams
import uk.gov.hmrc.auth.core.AuthorisationException
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.play.bootstrap.config.HttpAuditEvent

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class JsonErrorHandlerSpec
  extends AnyWordSpec
     with Matchers
     with ScalaFutures
     with MockitoSugar
     with LoneElement
     with TableDrivenPropertyChecks
     with EitherValues
     with Eventually {

  import ExecutionContext.Implicits.global

  "onServerError" should {
    "convert a NotFoundException to NotFound response and audit the error" in new Setup {
      val notFoundException = new NotFoundException("test")
      val createdDataEvent  = ExtendedDataEvent("auditSource", "auditType")
      when(
        httpAuditEvent.extendedEvent(
          eventType       = eqTo("ResourceNotFound"),
          transactionName = eqTo("Unexpected error"),
          request         = eqTo(requestHeader),
          detail          = eqTo(Json.obj("transactionFailureReason" -> notFoundException.getMessage)),
          truncationLog   = eqTo(None)
        )(any[HeaderCarrier]))
        .thenReturn(createdDataEvent)

      val result = jsonErrorHandler.onServerError(requestHeader, notFoundException)

      status(result)        shouldEqual NOT_FOUND
      contentAsJson(result) shouldEqual Json.obj("statusCode" -> NOT_FOUND, "message" -> "test")

      verify(auditConnector).sendExtendedEvent(eqTo(createdDataEvent))(any[HeaderCarrier], any[ExecutionContext])
    }

    "convert an AuthorisationException to Unauthorized response and audit the error" in new Setup {
      val authorisationException = new AuthorisationException("reason") {}
      val createdDataEvent       = ExtendedDataEvent("auditSource", "auditType")
      when(
        httpAuditEvent.extendedEvent(
          eventType       = eqTo("ClientError"),
          transactionName = eqTo("Unexpected error"),
          request         = eqTo(requestHeader),
          detail          = eqTo(Json.obj("transactionFailureReason" -> authorisationException.getMessage)),
          truncationLog   = eqTo(None)
        )(any[HeaderCarrier]))
        .thenReturn(createdDataEvent)

      val result = jsonErrorHandler.onServerError(requestHeader, authorisationException)

      status(result)        shouldEqual UNAUTHORIZED
      contentAsJson(result) shouldEqual Json.obj(
        "statusCode" -> UNAUTHORIZED,
        "message"    -> authorisationException.getMessage
      )

      verify(auditConnector).sendExtendedEvent(eqTo(createdDataEvent))(any[HeaderCarrier], any[ExecutionContext])
    }

    "convert an Exception to InternalServerError and audit the error" in new Setup {
      val exception        = new Exception("any application exception")
      val createdDataEvent = ExtendedDataEvent("auditSource", "auditType")
      when(
        httpAuditEvent.extendedEvent(
          eventType       = eqTo("ServerInternalError"),
          transactionName = eqTo("Unexpected error"),
          request         = eqTo(requestHeader),
          detail          = eqTo(Json.obj("transactionFailureReason" -> exception.getMessage)),
          truncationLog   = eqTo(None)
        )(any[HeaderCarrier]))
        .thenReturn(createdDataEvent)

      val result = jsonErrorHandler.onServerError(requestHeader, exception)

      status(result)        shouldEqual INTERNAL_SERVER_ERROR
      contentAsJson(result) shouldEqual Json.obj(
        "statusCode" -> INTERNAL_SERVER_ERROR,
        "message"    -> exception.getMessage
      )

      verify(auditConnector).sendExtendedEvent(eqTo(createdDataEvent))(any[HeaderCarrier], any[ExecutionContext])
    }

    "convert a JsValidationException to InternalServerError and audit the error" in new Setup {
      val exception        = new JsValidationException(GET, uri, classOf[Int], "json deserialization error")
      val createdDataEvent = ExtendedDataEvent("auditSource", "auditType")
      when(
        httpAuditEvent.extendedEvent(
          eventType       = eqTo("ServerValidationError"),
          transactionName = eqTo("Unexpected error"),
          request         = eqTo(requestHeader),
          detail          = eqTo(Json.obj("transactionFailureReason" -> exception.getMessage)),
          truncationLog   = eqTo(None)
        )(any[HeaderCarrier]))
        .thenReturn(createdDataEvent)

      val result = jsonErrorHandler.onServerError(requestHeader, exception)

      status(result)        shouldEqual INTERNAL_SERVER_ERROR
      contentAsJson(result) shouldEqual Json.obj(
        "statusCode" -> INTERNAL_SERVER_ERROR,
        "message"    -> exception.getMessage
      )

      verify(auditConnector).sendExtendedEvent(eqTo(createdDataEvent))(any[HeaderCarrier], any[ExecutionContext])
    }

    "convert a HttpException to responseCode from the exception and audit the error" in new Setup {
      val responseCode     = randomErrorStatusCode()
      val exception        = new HttpException("error message", responseCode)
      val createdDataEvent = ExtendedDataEvent("auditSource", "auditType")
      when(
        httpAuditEvent.extendedEvent(
          eventType       = eqTo("ServerInternalError"),
          transactionName = eqTo("Unexpected error"),
          request         = eqTo(requestHeader),
          detail          = eqTo(Json.obj("transactionFailureReason" -> exception.getMessage)),
          truncationLog   = eqTo(None)
        )(any[HeaderCarrier]))
        .thenReturn(createdDataEvent)

      val result = jsonErrorHandler.onServerError(requestHeader, exception)

      status(result)        shouldEqual responseCode
      contentAsJson(result) shouldEqual Json.obj(
        "statusCode" -> responseCode,
        "message"    -> exception.getMessage
      )

      verify(auditConnector).sendExtendedEvent(eqTo(createdDataEvent))(any[HeaderCarrier], any[ExecutionContext])
    }

    "convert an UpstreamErrorResponse to reportAs from the exception and audit the error" in new Setup {
      val reportAs         = randomErrorStatusCode()
      val exception        = UpstreamErrorResponse("error message", randomErrorStatusCode(), reportAs)
      val createdDataEvent = ExtendedDataEvent("auditSource", "auditType")
      when(
        httpAuditEvent.extendedEvent(
          eventType       = eqTo("ServerInternalError"),
          transactionName = eqTo("Unexpected error"),
          request         = eqTo(requestHeader),
          detail          = eqTo(Json.obj("transactionFailureReason" -> exception.getMessage)),
          truncationLog   = eqTo(None)
        )(any[HeaderCarrier]))
        .thenReturn(createdDataEvent)

      val result = jsonErrorHandler.onServerError(requestHeader, exception)

      status(result)        shouldEqual reportAs
      contentAsJson(result) shouldEqual Json.obj(
        "statusCode" -> reportAs,
        "message"    -> exception.getMessage
      )

      verify(auditConnector).sendExtendedEvent(eqTo(createdDataEvent))(any[HeaderCarrier], any[ExecutionContext])
    }

    "NOT suppress AuthorisationException error messages when suppression enabled" in new Setup(
      config = Map(
        "appName" -> "myApp",
        "bootstrap.errorHandler.suppress5xxErrorMessages" -> true
      )
    ) {
      val realMessage      = "real message"
      val exception        = new AuthorisationException(realMessage) {}
      val createdDataEvent = ExtendedDataEvent("auditSource", "auditType")
      when(httpAuditEvent.extendedEvent(any, any, any, any, any)(any))
        .thenReturn(createdDataEvent)

      val result = jsonErrorHandler.onServerError(requestHeader, exception)

      status(result)        shouldEqual UNAUTHORIZED
      contentAsJson(result) shouldEqual Json.obj(
        "statusCode" -> UNAUTHORIZED,
        "message"    -> realMessage
      )
    }

    "NOT suppress HttpException error messages when suppression enabled" in new Setup(
      config = Map(
        "appName" -> "myApp",
        "bootstrap.errorHandler.suppress5xxErrorMessages" -> true
      )
    ) {
      val realMessage      = "real message"
      val realStatusCode   = 500
      val exception        = new HttpException(realMessage, realStatusCode)
      val createdDataEvent = ExtendedDataEvent("auditSource", "auditType")
      when(httpAuditEvent.extendedEvent(any, any, any, any, any)(any))
        .thenReturn(createdDataEvent)

      val result = jsonErrorHandler.onServerError(requestHeader, exception)

      status(result)        shouldEqual realStatusCode
      contentAsJson(result) shouldEqual Json.obj(
        "statusCode" -> realStatusCode,
        "message"    -> realMessage
      )
    }

    "suppress Upstream error messages when suppression enabled" in new Setup(
      config = Map(
        "appName" -> "myApp",
        "bootstrap.errorHandler.suppress5xxErrorMessages" -> true
      )
    ) {
      val reportAs         = randomErrorStatusCode()
      val upstreamError    = randomErrorStatusCode()
      val exception        = UpstreamErrorResponse("error message", upstreamError, reportAs)
      val createdDataEvent = ExtendedDataEvent("auditSource", "auditType")
      when(httpAuditEvent.extendedEvent(any, any, any, any, any)(any))
        .thenReturn(createdDataEvent)

      val result = jsonErrorHandler.onServerError(requestHeader, exception)

      status(result)        shouldEqual reportAs
      contentAsJson(result) shouldEqual Json.obj(
        "statusCode" -> reportAs,
        "message"    -> s"UpstreamErrorResponse: $upstreamError"
      )
    }

    "suppress Other error messages when suppression enabled" in new Setup(
      config = Map(
        "appName" -> "myApp",
        "bootstrap.errorHandler.suppress5xxErrorMessages" -> true
      )
    ) {
      val exception        = new RuntimeException("real message")
      val createdDataEvent = ExtendedDataEvent("auditSource", "auditType")
      when(httpAuditEvent.extendedEvent(any, any, any, any, any)(any))
        .thenReturn(createdDataEvent)

      val result = jsonErrorHandler.onServerError(requestHeader, exception)

      status(result)        shouldEqual INTERNAL_SERVER_ERROR
      contentAsJson(result) shouldEqual Json.obj(
        "statusCode" -> INTERNAL_SERVER_ERROR,
        "message"    -> "Other error"
      )
    }

    "log a warning for upstream code in the warning list" when {
      class WarningSetup(upstreamWarnStatuses: Seq[Int]) extends Setup(
        config = Map(
          "appName" -> "myApp",
          "bootstrap.errorHandler.warnOnly.statusCodes" -> upstreamWarnStatuses
        )
      )

      def withCaptureOfLoggingFrom(loggerLike: LoggerLike)(body: (=> List[ILoggingEvent]) => Unit): Unit = {
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

      val errorScenarios = Table(
        ("errorMessage", "expectedResponseMessage"),
        ("Invalid Json: No content to map due to end-of-input\n at [Source", "Invalid Json: No content to map due to end-of-input\n at [Source"),
        ("Invalid Json: Unrecognized token 'blah': was expecting", "Invalid Json: Unrecognized token 'REDACTED': was expecting"),
        ("Json validation error List((obj.greeting,List(JsonValidationError(List(error.path.missing),WrappedArray()))))", "Json validation error List((obj.greeting,List(JsonValidationError(List(error.path.missing),WrappedArray()))))"),
        ("Cannot parse parameter aBoolean as Boolean: should be true, false, 0 or 1", "Cannot parse parameter aBoolean as Boolean: should be true, false, 0 or 1"),
        ("Missing parameter: otherId", "Missing parameter: otherId"),
        ("Cannot parse parameter id as Int: For input string: \"blah\"", "Cannot parse parameter id as Int: For input string: \"REDACTED\""),
        ("Cannot parse parameter aChar with value '123' as Char: aChar must be exactly one digit in length.", "Cannot parse parameter aChar with value 'REDACTED' as Char: aChar must be exactly one digit in length."),
        ("some unknown message", "bad request, cause: REDACTED")
      )

      forAll(errorScenarios) { (errorMessage, expectedResponseMessage) =>
        val createdDataEvent = ExtendedDataEvent("auditSource", "auditType")
        when(
          httpAuditEvent.extendedEvent(
            eventType       = eqTo("ServerValidationError"),
            transactionName = eqTo("Request bad format exception"),
            request         = eqTo(requestHeader),
            detail          = eqTo(JsObject.empty),
            truncationLog   = eqTo(None)
          )(any[HeaderCarrier]))
          .thenReturn(createdDataEvent)

        val result = jsonErrorHandler.onClientError(requestHeader, BAD_REQUEST, errorMessage)

        status(result) shouldEqual BAD_REQUEST
        contentAsJson(result) shouldEqual Json.obj("statusCode" -> BAD_REQUEST, "message" -> expectedResponseMessage)

        verify(auditConnector).sendExtendedEvent(eqTo(createdDataEvent))(any[HeaderCarrier], any[ExecutionContext])
      }
    }

    "audit an error and return json response for 404 including requested path" in new Setup {
      val createdDataEvent = ExtendedDataEvent("auditSource", "auditType")
      when(
        httpAuditEvent.extendedEvent(
          eventType       = eqTo("ResourceNotFound"),
          transactionName = eqTo("Resource Endpoint Not Found"),
          request         = eqTo(requestHeader),
          detail          = eqTo(JsObject.empty),
          truncationLog   = eqTo(None)
        )(any[HeaderCarrier]))
        .thenReturn(createdDataEvent)

      val result = jsonErrorHandler.onClientError(requestHeader, NOT_FOUND, "some message we want to override")

      status(result) shouldEqual NOT_FOUND
      contentAsJson(result) shouldEqual Json
        .obj("statusCode" -> NOT_FOUND, "message" -> "URI not found", "requested" -> uri)

      verify(auditConnector).sendExtendedEvent(eqTo(createdDataEvent))(any[HeaderCarrier], any[ExecutionContext])
    }

    "audit an error and return json response for 4xx except 404 and 400" in new Setup {
      (401 to 499).filter(_ != 404).foreach { statusCode =>
        val createdDataEvent = ExtendedDataEvent("auditSource", "auditType")
        when(
          httpAuditEvent.extendedEvent(
            eventType       = eqTo("ClientError"),
            transactionName = eqTo(s"A client error occurred, status: $statusCode"),
            request         = eqTo(requestHeader),
            detail          = eqTo(JsObject.empty),
            truncationLog   = eqTo(None)
          )(any[HeaderCarrier]))
          .thenReturn(createdDataEvent)

        val errorMessage = "unauthorized"

        val result = jsonErrorHandler.onClientError(requestHeader, statusCode, errorMessage)

        status(result)        shouldEqual statusCode
        contentAsJson(result) shouldEqual Json.obj("statusCode" -> statusCode, "message" -> errorMessage)

        verify(auditConnector).sendExtendedEvent(eqTo(createdDataEvent))(any[HeaderCarrier], any[ExecutionContext])
      }
    }

    "suppress messages when suppression enabled" in new Setup(
      config = Map(
        "appName" -> "myApp",
        "bootstrap.errorHandler.suppress4xxErrorMessages" -> true
      )
    ) {
      val errorMessage = "real message"

      val createdDataEvent = ExtendedDataEvent("auditSource", "auditType")
      when(httpAuditEvent.extendedEvent(any, any, any, any, any)(any))
        .thenReturn(createdDataEvent)

      (400 to 499).filter(_ != 404).foreach { statusCode =>
        val expectedMessage = if (statusCode == 400) "Bad request" else "Other error"

        val result = jsonErrorHandler.onClientError(requestHeader, statusCode, errorMessage)

        status(result)        shouldEqual statusCode
        contentAsJson(result) shouldEqual Json.obj(
          "statusCode" -> statusCode,
          "message"    -> expectedMessage
        )
      }
    }
  }

  // these are the assumptions the redacting logic in JsonErrorHandler expects from plaexpectations on play, such that J
  "play" should {
    implicit val system = ActorSystem()

    val errorHandler = new HttpErrorHandler {
      override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] =
        Future.successful(Results.Status(statusCode)(message))
      override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] =
        Future.successful(Results.InternalServerError(exception.getMessage))
    }

    "return Invalid Json error when an empty body is provided" in new JsonSetup {
      errorResponse(parsers.json, "") should startWith("Invalid Json:")
    }

    "return Invalid Json error when an invalid json is provided" in new JsonSetup {
      errorResponse(parsers.json, "blah") should startWith("Invalid Json:")
    }

    "return Json validation error when a json with wrong schema is provided" in new JsonSetup {
      case class Test(greeting: String)
      errorResponse(parsers.json[Test](Json.reads[Test]), "{}")  should startWith("Json validation error")
    }

    "return Json validation error without original values" in new JsonSetup {
      case class User(id: Int)
      val id = UUID.randomUUID().toString
      val errorMessage = errorResponse(parsers.json[User](Json.reads[User]), s"""{"id": "$id"}""")
      errorMessage should startWith("Json validation error")
      errorMessage should not include id
    }

    "return error for missing query param" in {
      RouteParams(Map.empty, Map.empty).fromQuery("key")(QueryStringBindable.bindableInt).value shouldBe Left("Missing parameter: key")
    }

    "return error for boolean parsing" in {
      PathBindable.bindableBoolean.bind("key", "value") shouldBe Left("Cannot parse parameter key as Boolean: should be true, false, 0 or 1")
      QueryStringBindable.bindableBoolean.bind("key", Map("key" -> Seq("value"))) shouldBe Some(Left("Cannot parse parameter key as Boolean: should be true, false, 0 or 1"))
    }

    "return error for char parsing" in {
      PathBindable.bindableChar.bind("key", "value") shouldBe Left("Cannot parse parameter key with value 'value' as Char: key must be exactly one digit in length.")
      QueryStringBindable.bindableChar.bind("key", Map("key" -> Seq("value"))) shouldBe Some(Left("Cannot parse parameter key with value 'value' as Char: key must be exactly one digit in length."))
    }

    "return error for other data types parsing" in {
      PathBindable.bindableInt.bind("key", "value") shouldBe Left("Cannot parse parameter key as Int: For input string: \"value\"")
      QueryStringBindable.bindableInt.bind("key", Map("key" -> Seq("value"))) shouldBe Some(Left("Cannot parse parameter key as Int: For input string: \"value\""))
    }

    trait JsonSetup {
      val fakeRequest = FakeRequest().withHeaders(PlayHeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
      val parsers = PlayBodyParsers(eh = errorHandler)
      def errorResponse[A](parser: BodyParser[A], body: String): String =
        parser(fakeRequest).run(ByteString(body)).futureValue.left.value.body.dataStream.runReduce(_ ++ _).futureValue.utf8String
    }
  }

  private class Setup(
    config: Map[String, Any] = Map("appName" -> "myApp")
  ) {
    val uri           = "some-uri"
    val requestHeader = FakeRequest(GET, uri)

    val auditConnector = mock[AuditConnector]
    when(auditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.successful(Success))

    val httpAuditEvent = mock[HttpAuditEvent](withSettings.lenient)

    val configuration    =
      Configuration.from(config).withFallback(Configuration(ConfigFactory.load()))

    lazy val jsonErrorHandler = new JsonErrorHandler(auditConnector, httpAuditEvent, configuration)

    def randomErrorStatusCode(): Int =
      400 + Random.nextInt(200)
  }
}
