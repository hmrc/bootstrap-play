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

package uk.gov.hmrc.play.bootstrap.backend.http

import play.api.{Configuration, Logger}
import play.api.http.HttpErrorHandler
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.mvc.Results._
import play.api.mvc.{RequestHeader, Result}
import uk.gov.hmrc.auth.core.AuthorisationException
import uk.gov.hmrc.http._
import uk.gov.hmrc.mdc.RequestMdc
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.HttpAuditEvent
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class JsonErrorHandler @Inject()(
  auditConnector: AuditConnector,
  httpAuditEvent: HttpAuditEvent,
  configuration : Configuration
)(implicit ec: ExecutionContext
) extends HttpErrorHandler
     with BackendHeaderCarrierProvider {

  import httpAuditEvent.dataEvent

  private val logger = Logger(getClass)

  /**
    * `upstreamWarnStatuses` is used to determine the log level for exceptions
    * relating to a HttpResponse. You can set this value in your config as
    * a list of integers representing response codes that should log at a
    * warning level rather an error level.
    *
    * e.g. bootstrap.errorHandler.warnOnly.statusCodes=[400,404,502]
    *
    * This is used to reduce the number of noise the number of duplicated alerts
    * for a microservice.
    */
  protected val upstreamWarnStatuses: Seq[Int] =
    configuration.get[Seq[Int]]("bootstrap.errorHandler.warnOnly.statusCodes")

  protected val suppress4xxErrorMessages: Boolean =
    configuration.get[Boolean]("bootstrap.errorHandler.suppress4xxErrorMessages")

  protected val suppress5xxErrorMessages: Boolean =
    configuration.get[Boolean]("bootstrap.errorHandler.suppress5xxErrorMessages")

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    // Use `Try` since if an invalid url is provided, then we will come here, but without a request id
    // (also won't have hit the MdcFilter)
    Try(request.id).map(RequestMdc.initMdc)

    implicit val headerCarrier: HeaderCarrier = hc(request)
    val result = statusCode match {
      case NOT_FOUND =>
        auditConnector.sendEvent(
          dataEvent(
            eventType       = "ResourceNotFound",
            transactionName = "Resource Endpoint Not Found",
            request         = request,
            detail          = Map.empty
          )
        )
        NotFound(toJson(ErrorResponse(NOT_FOUND, "URI not found", requested = Some(request.path))))

      case BAD_REQUEST =>
        auditConnector.sendEvent(
          dataEvent(
            eventType       = "ServerValidationError",
            transactionName = "Request bad format exception",
            request         = request,
            detail          = Map.empty
          )
        )
        def constructErrorMessage(input: String): String = {
          val unrecognisedTokenJsonError = "^Invalid Json: Unrecognized token '(.*)':.*".r
          val invalidJson                = "^(?s)Invalid Json:.*".r
          val jsonValidationError        = "^Json validation error.*".r
          val booleanParsingError        = "^Cannot parse parameter .* as Boolean: should be true, false, 0 or 1$".r
          val missingParameterError      = "^Missing parameter:.*".r
          val characterParseError        = "^Cannot parse parameter .* with value '(.*)' as Char: .* must be exactly one digit in length.$".r
          val parameterParseError        = "^Cannot parse parameter .* as .*: For input string: \"(.*)\"$".r
          input match {
            case unrecognisedTokenJsonError(toBeRedacted) => input.replace(toBeRedacted, "REDACTED")
            case invalidJson()
               | jsonValidationError()
               | booleanParsingError()
               | missingParameterError()                  => input
            case characterParseError(toBeRedacted)        => input.replace(toBeRedacted, "REDACTED")
            case parameterParseError(toBeRedacted)        => input.replace(toBeRedacted, "REDACTED")
            case _                                        => "bad request, cause: REDACTED"
          }
        }
        val msg =
          if (suppress4xxErrorMessages) "Bad request"
          else constructErrorMessage(message)

        BadRequest(toJson(ErrorResponse(BAD_REQUEST, msg)))

      case _ =>
        auditConnector.sendEvent(
          dataEvent(
            eventType       = "ClientError",
            transactionName = s"A client error occurred, status: $statusCode",
            request         = request,
            detail          = Map.empty
          )
        )

        val msg =
          if (suppress4xxErrorMessages) "Other error"
          else message

        Status(statusCode)(toJson(ErrorResponse(statusCode, msg)))
    }
    Future.successful(result)
  }

  override def onServerError(request: RequestHeader, ex: Throwable): Future[Result] = {
    RequestMdc.initMdc(request.id)
    implicit val headerCarrier: HeaderCarrier = hc(request)

    val eventType = ex match {
      case _: NotFoundException      => "ResourceNotFound"
      case _: AuthorisationException => "ClientError"
      case _: JsValidationException  => "ServerValidationError"
      case _                         => "ServerInternalError"
    }

    val errorResponse = ex match {
      case e: AuthorisationException =>
        logException(e, 401, request)
        // message is not suppressed here since needs to be forwarded
        ErrorResponse(401, e.getMessage)

      case e: HttpException =>
        logException(e, e.responseCode, request)
        // message is not suppressed here since HttpException exists to define returned message
        ErrorResponse(e.responseCode, e.getMessage)

      case e: UpstreamErrorResponse =>
        logExceptionWithMessageLimit(e, e.statusCode, request)
        val msg =
          if (suppress5xxErrorMessages) s"UpstreamErrorResponse: ${e.statusCode}"
          else e.getMessage
        ErrorResponse(e.reportAs, msg)

      case e: Throwable =>
        logger.error(s"! Internal server error, for (${request.method}) [${request.uri}] -> ", e)
        val msg =
          if (suppress5xxErrorMessages) "Other error"
          else e.getMessage
        ErrorResponse(INTERNAL_SERVER_ERROR, msg)
    }

    auditConnector.sendEvent(
      dataEvent(
        eventType       = eventType,
        transactionName = "Unexpected error",
        request         = request,
        detail          = Map("transactionFailureReason" -> ex.getMessage)
      )
    )

    Future.successful(new Status(errorResponse.statusCode)(Json.toJson(errorResponse)))
  }

  protected val maxLogMessageLength: Int =
    configuration.get[Int]("bootstrap.errorHandler.maxLogMessageLength")

  protected val desiredLogMessageLength: Int =
    configuration.get[Int]("bootstrap.errorHandler.logMessageLength")

  private def logExceptionWithMessageLimit(exception: Exception, responseCode: Int, request: RequestHeader): Unit = {
    val suffix      = "...[truncated]"
    val limit       = math.min(desiredLogMessageLength, maxLogMessageLength)
    val baseMessage = s"${request.method} ${request.uri} failed with ${exception.getClass.getName}: ${exception.getMessage}"

    val msg = {
      if (baseMessage.length <= limit)
        baseMessage
      else
        baseMessage.take(math.max(limit - suffix.length, 0)) + suffix // avoids negative values
    }

    if (upstreamWarnStatuses.contains(responseCode))
      logger.warn(msg, exception)
    else
      logger.error(msg, exception)
  }

  private def logException(exception: Exception, responseCode: Int, request: RequestHeader): Unit = {
    val msg = s"${request.method} ${request.uri} failed with ${exception.getClass.getName}: ${exception.getMessage}"
    if (upstreamWarnStatuses.contains(responseCode))
      logger.warn(msg, exception)
    else
      logger.error(msg, exception)
  }
}
