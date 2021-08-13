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

import javax.inject.Inject
import play.api.{Configuration, Logger}
import play.api.http.HttpErrorHandler
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.mvc.Results._
import play.api.mvc.{RequestHeader, Result}
import uk.gov.hmrc.auth.core.AuthorisationException
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.HttpAuditEvent
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider

import scala.concurrent.{ExecutionContext, Future}

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
  protected val upstreamWarnStatuses: Seq[Int] = configuration.get[Seq[Int]]("bootstrap.errorHandler.warnOnly.statusCodes")

  /**
    * This method is used to derive the `code` field of JSON error responses
    * based upon the `statusCode` and error `message` provided.
    *
    * You can override this method to return error codes that are specific to your API.
    *
    * @param statusCode The status code that will be returned in the response
    * @param message The `message` that will be returned in the response
    * @return An error code string that will be used in the `code` field of the response
    */
  protected def errorCodeFor(statusCode: Int, message: String): String =
    statusCode match {
      case BAD_REQUEST                   => ErrorCodes.BAD_REQUEST
      case UNAUTHORIZED                  => ErrorCodes.UNAUTHORIZED
      case FORBIDDEN                     => ErrorCodes.FORBIDDEN
      case NOT_FOUND                     => ErrorCodes.MATCHING_RESOURCE_NOT_FOUND
      case METHOD_NOT_ALLOWED            => ErrorCodes.METHOD_NOT_ALLOWED
      case NOT_ACCEPTABLE                => ErrorCodes.ACCEPT_HEADER_INVALID
      case TOO_MANY_REQUESTS             => ErrorCodes.MESSAGE_THROTTLED_OUT
      case INTERNAL_SERVER_ERROR         => ErrorCodes.INTERNAL_SERVER_ERROR
      case NOT_IMPLEMENTED               => ErrorCodes.NOT_IMPLEMENTED
      case GATEWAY_TIMEOUT               => ErrorCodes.GATEWAY_TIMEOUT
      case other if isClientError(other) => ErrorCodes.CLIENT_ERROR
      case other if isServerError(other) => ErrorCodes.SERVER_ERROR
    }

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    implicit val headerCarrier: HeaderCarrier = hc(request)
    val result = statusCode match {
      case NOT_FOUND =>
        auditConnector.sendEvent(
          dataEvent(
            eventType = "ResourceNotFound",
            transactionName = "Resource Endpoint Not Found",
            request = request,
            detail = Map.empty
          )
        )
        val notFoundMessage = "URI not found"
        val errorCode = errorCodeFor(statusCode, notFoundMessage)
        NotFound(toJson(ErrorResponse(NOT_FOUND, errorCode, notFoundMessage, requested = Some(request.path))))
      case BAD_REQUEST =>
        auditConnector.sendEvent(
          dataEvent(
            eventType = "ServerValidationError",
            transactionName = "Request bad format exception",
            request = request,
            detail = Map.empty
          )
        )
        def constructErrorMessage(input: String): String = {
          val unrecognisedTokenJsonError = "^Invalid Json: Unrecognized token '(.*)':.*".r
          val invalidJson = "^(?s)Invalid Json:.*".r
          val jsonValidationError = "^Json validation error.*".r
          val booleanParsingError = "^Cannot parse parameter .* as Boolean: should be true, false, 0 or 1$".r
          val missingParameterError = "^Missing parameter:.*".r
          val characterParseError = "^Cannot parse parameter .* with value '(.*)' as Char: .* must be exactly one digit in length.$".r
          val parameterParseError = "^Cannot parse parameter .* as .*: For input string: \"(.*)\"$".r
          input match {
            case unrecognisedTokenJsonError(toBeRedacted) => input.replaceAllLiterally(toBeRedacted, "REDACTED")
            case invalidJson() | jsonValidationError() | booleanParsingError() | missingParameterError() => input
            case characterParseError(toBeRedacted) => input.replaceAllLiterally(toBeRedacted, "REDACTED")
            case parameterParseError(toBeRedacted) => input.replaceAllLiterally(toBeRedacted, "REDACTED")
            case _ => "bad request, cause: REDACTED"
          }
        }
        val errorMessage = constructErrorMessage(message)
        val errorCode = errorCodeFor(BAD_REQUEST, errorMessage)
        BadRequest(toJson(ErrorResponse(BAD_REQUEST, errorCode, errorMessage)))
      case _ =>
        auditConnector.sendEvent(
          dataEvent(
            eventType = "ClientError",
            transactionName = s"A client error occurred, status: $statusCode",
            request = request,
            detail = Map.empty
          )
        )
        Status(statusCode)(toJson(ErrorResponse(statusCode, errorCodeFor(statusCode, message), message)))
    }
    Future.successful(result)
  }

  override def onServerError(request: RequestHeader, ex: Throwable): Future[Result] = {
    implicit val headerCarrier: HeaderCarrier = hc(request)

    val message = s"! Internal server error, for (${request.method}) [${request.uri}] -> "
    val eventType = ex match {
      case _: NotFoundException      => "ResourceNotFound"
      case _: AuthorisationException => "ClientError"
      case _: JsValidationException  => "ServerValidationError"
      case _                         => "ServerInternalError"
    }

    val errorResponse = ex match {
      case e: AuthorisationException =>
        logger.error(message, e)
        val errorCode = errorCodeFor(401, e.getMessage)
        ErrorResponse(401, errorCode, e.getMessage)
      case e: HttpException =>
        logException(e, e.responseCode)
        val errorCode = errorCodeFor(e.responseCode, e.getMessage)
        ErrorResponse(e.responseCode, errorCode, e.getMessage)
      case e: UpstreamErrorResponse =>
        logException(e, e.statusCode)
        val errorCode = errorCodeFor(e.reportAs, e.getMessage)
        ErrorResponse(e.reportAs, errorCode, e.getMessage)
      case e: Throwable =>
        logger.error(message, e)
        val errorCode = errorCodeFor(INTERNAL_SERVER_ERROR, e.getMessage)
        ErrorResponse(INTERNAL_SERVER_ERROR, errorCode, e.getMessage)
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

  private def logException(exception: Exception, responseCode: Int): Unit =
    if (upstreamWarnStatuses contains responseCode)
      logger.warn(exception.getMessage, exception)
    else
      logger.error(exception.getMessage, exception)
}
