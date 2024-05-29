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

package uk.gov.hmrc.play.bootstrap.frontend.http

import play.api.http.HeaderNames.CACHE_CONTROL
import play.api.http.HttpErrorHandler
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound}
import play.api.mvc.{Request, RequestHeader, Result, Results}
import play.api.{Logger, PlayException}
import play.twirl.api.Html

import scala.concurrent.Future
import scala.language.implicitConversions

abstract class FrontendErrorHandler extends HttpErrorHandler with I18nSupport {

  private val logger = Logger(getClass)

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] =
    statusCode match {
      case play.mvc.Http.Status.BAD_REQUEST => Future.successful(BadRequest(badRequestTemplate(request)))
      case play.mvc.Http.Status.NOT_FOUND   => Future.successful(NotFound(notFoundTemplate(request)))
      case _                                => Future.successful(Results.Status(statusCode)(fallbackClientErrorTemplate(request)))
    }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    logError(request, exception)
    Future.successful(resolveError(request, exception))
  }

  private implicit def rhToRequest(rh: RequestHeader): Request[_] = Request(rh, "")

  def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html

  def badRequestTemplate(implicit request: Request[_]): Html =
    standardErrorTemplate(
      Messages("global.error.badRequest400.title"),
      Messages("global.error.badRequest400.heading"),
      Messages("global.error.badRequest400.message"))

  def notFoundTemplate(implicit request: Request[_]): Html =
    standardErrorTemplate(
      Messages("global.error.pageNotFound404.title"),
      Messages("global.error.pageNotFound404.heading"),
      Messages("global.error.pageNotFound404.message"))

  def fallbackClientErrorTemplate(implicit request: Request[_]): Html =
    standardErrorTemplate(
      Messages("global.error.fallbackClientError4xx.title"),
      Messages("global.error.fallbackClientError4xx.heading"),
      Messages("global.error.fallbackClientError4xx.message"))

  def internalServerErrorTemplate(implicit request: Request[_]): Html =
    standardErrorTemplate(
      Messages("global.error.InternalServerError500.title"),
      Messages("global.error.InternalServerError500.heading"),
      Messages("global.error.InternalServerError500.message")
    )

  private def logError(request: RequestHeader, ex: Throwable): Unit =
    logger.error(
      """
          |
          |! %sInternal server error, for (%s) [%s] ->
          | """.stripMargin.format(ex match {
        case p: PlayException => "@" + p.id + " - "
        case _                => ""
      }, request.method, request.uri),
      ex
    )

  def resolveError(rh: RequestHeader, ex: Throwable): Result =
    ex match {
      case ApplicationException(result, _) => result
      case _ =>
        InternalServerError(internalServerErrorTemplate(rh)).withHeaders(CACHE_CONTROL -> "no-cache")
    }
}

case class ApplicationException(result: Result, message: String) extends Exception(message)
