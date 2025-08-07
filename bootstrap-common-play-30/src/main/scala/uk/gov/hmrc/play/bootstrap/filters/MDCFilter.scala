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

import org.apache.pekko.stream.Materializer
import play.api.Configuration
import play.api.mvc.{Filter, RequestHeader, Result}
import play.api.routing.Router
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}
import uk.gov.hmrc.mdc.RequestMdc

import scala.concurrent.{ExecutionContext, Future}

trait MDCFilter extends Filter {

  val mat: Materializer
  val configuration: Configuration
  implicit val ec: ExecutionContext

  protected def hc(implicit rh: RequestHeader): HeaderCarrier

  private val includeHandler =
    configuration.get[Boolean]("bootstrap.mdc.includeHandler")

  override def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
    val headerCarrier = hc(rh)

    val data =
      Map(
        HeaderNames.xRequestId    -> headerCarrier.requestId.fold("-")(_.value),
        HeaderNames.xSessionId    -> headerCarrier.sessionId.fold("-")(_.value),
        HeaderNames.xForwardedFor -> headerCarrier.forwarded.fold("-")(_.value)
      ) ++
        (if (includeHandler) Map("handler" -> handler(rh)) else Map.empty)

    RequestMdc.add(rh.id, data)

    f(rh)
  }

  private def handler(rh: RequestHeader): String = {
    import Router.RequestImplicits._
    rh.handlerDef match {
      case Some(handlerDef) => s"${handlerDef.verb} ${handlerDef.controller}.${handlerDef.method}"
      case _                => "-" // e.g. 404s
    }
  }
}
