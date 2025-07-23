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
import play.api.i18n.{I18nSupport, Lang, LangImplicits, Messages}
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound}
import play.api.mvc.{RequestHeader, Result, Results}
import play.api.{Logger, PlayException}
import play.twirl.api.Html
import uk.gov.hmrc.mdc.RequestMdc

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

abstract class FrontendErrorHandler
  extends HttpErrorHandler
     with I18nSupport
     with LangImplicits {

  private val logger = Logger(getClass)

  protected implicit val ec: ExecutionContext

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    // Use `Try` since if an invalid url is provided, then we will come here, but without a request id
    // (also won't have hit the MdcFilter)
    Try(request.id).map(RequestMdc.initMdc)

    statusCode match {
      case play.mvc.Http.Status.BAD_REQUEST => badRequestTemplate(request).map(BadRequest(_))
      case play.mvc.Http.Status.NOT_FOUND   => notFoundTemplate(request).map(NotFound(_))
      case _                                => fallbackClientErrorTemplate(request).map(Results.Status(statusCode)(_))
    }
  }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    RequestMdc.initMdc(request.id)
    logError(request, exception)
    resolveError(request, exception)
  }

  /** To be provided to wire up to a View.
   *  The page title should be modified to adhere to the organisation guidelines - See Default Error Handling section in README */
  def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: RequestHeader): Future[Html]

  // workaround for https://github.com/playframework/playframework/issues/8113
  // Some errors occur before the cookie is parsed, in which case request2Messages (which requires lang from cookie) will fail
  implicit override def request2Messages(implicit request: RequestHeader): Messages =
    // An alternative could be to use `implicit val rh2: RequestHeader = requestFactory.copyRequest(rh)` (which doesn't copy but actually populates attrs from headers)
    // but would be a breaking change to add dependency on requestFactory
    Try(super.request2Messages)
      .getOrElse(lang2Messages(Lang.defaultLang))

  def badRequestTemplate(implicit request: RequestHeader): Future[Html] =
    standardErrorTemplate(
      Messages("global.error.badRequest400.title"),
      Messages("global.error.badRequest400.heading"),
      Messages("global.error.badRequest400.message")
    )

  def notFoundTemplate(implicit request: RequestHeader): Future[Html] =
    standardErrorTemplate(
      Messages("global.error.pageNotFound404.title"),
      Messages("global.error.pageNotFound404.heading"),
      Messages("global.error.pageNotFound404.message")
    )

  def fallbackClientErrorTemplate(implicit request: RequestHeader): Future[Html] =
    standardErrorTemplate(
      Messages("global.error.fallbackClientError4xx.title"),
      Messages("global.error.fallbackClientError4xx.heading"),
      Messages("global.error.fallbackClientError4xx.message")
    )

  def internalServerErrorTemplate(implicit request: RequestHeader): Future[Html] =
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
          | """
        .stripMargin
        .format(
          ex match {
            case p: PlayException => "@" + p.id + " - "
            case _                => ""
          },
          request.method,
          request.uri
        ),
      ex
    )

  def resolveError(rh: RequestHeader, ex: Throwable): Future[Result] =
    ex match {
      case ApplicationException(result, _) => Future.successful(result)
      case _ =>
        internalServerErrorTemplate(rh)
          .map(html => InternalServerError(html).withHeaders(CACHE_CONTROL -> "no-cache"))
    }
}

case class ApplicationException(
  result : Result,
  message: String
) extends Exception(message)
