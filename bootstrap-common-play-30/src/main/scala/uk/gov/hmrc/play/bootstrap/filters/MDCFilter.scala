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
import org.slf4j.MDC
import play.api.Configuration
import play.api.mvc.{Filter, RequestHeader, Result}
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}

import scala.concurrent.{ExecutionContext, Future}

trait MDCFilter extends Filter {
  val mat: Materializer
  val configuration: Configuration
  implicit val ec: ExecutionContext

  protected def hc(implicit rh: RequestHeader): HeaderCarrier

  override def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
    val headerCarrier = hc(rh)

    val data = Set(
      headerCarrier.requestId.map(HeaderNames.xRequestId    -> _.value),
      headerCarrier.sessionId.map(HeaderNames.xSessionId    -> _.value),
      headerCarrier.forwarded.map(HeaderNames.xForwardedFor -> _.value)
    ).flatten

    data.foreach {
      case (k, v) =>
        MDC.put(k, v)
    }

    f(rh)
  }
}
