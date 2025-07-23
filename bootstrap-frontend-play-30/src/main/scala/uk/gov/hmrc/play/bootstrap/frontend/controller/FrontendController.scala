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

package uk.gov.hmrc.play.bootstrap.frontend.controller

import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.{Utf8MimeTypes, WithJsonBody, WithUrlEncodedOnlyFormBinding}
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.mdc.RequestMdc

import scala.concurrent.{ExecutionContext, Future}

trait FrontendBaseController
  extends MessagesBaseController
     with Utf8MimeTypes
     with WithJsonBody
     with FrontendHeaderCarrierProvider {

  // same as default, but initialises Mdc for request
  override def Action: ActionBuilder[MessagesRequest, AnyContent] = {
    val defaultActionBuilder = controllerComponents.messagesActionBuilder.compose(controllerComponents.actionBuilder)
    new ActionBuilder[MessagesRequest, AnyContent] {
      override protected val executionContext: ExecutionContext =
        controllerComponents.executionContext

      override def invokeBlock[T](
        request: Request[T],
        block  : MessagesRequest[T] => Future[Result]
      ): Future[Result] = {
        RequestMdc.initMdc(request.id)
        defaultActionBuilder
          .invokeBlock(request, block)
      }

      override val parser: BodyParser[AnyContent] =
        defaultActionBuilder.parser
    }
  }
}

abstract class FrontendController(
  override val controllerComponents: MessagesControllerComponents
) extends FrontendBaseController
     with WithUrlEncodedOnlyFormBinding

trait FrontendHeaderCarrierProvider {
  implicit protected def hc(implicit request: RequestHeader): HeaderCarrier = {
    RequestMdc.initMdc(request.id)
    HeaderCarrierConverter.fromRequestAndSession(request, request.session)
  }
}
