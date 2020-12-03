/*
 * Copyright 2020 HM Revenue & Customs
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

import akka.stream.Materializer
import javax.inject.{Inject, Named, Singleton}
import org.slf4j.MDC
import play.api.Configuration
import play.api.mvc.{Filter, RequestHeader, Result}
import uk.gov.hmrc.http.HeaderNames
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.Future

@Singleton
class MDCFilter @Inject()(
  val mat: Materializer,
  config: Configuration,
  @Named("appName") appName: String
) extends Filter {

  private val dateFormat: Option[String] = config.getOptional[String]("logger.json.dateformat")

  private val extras: Set[(String, String)] =
    Set("appName" -> appName) ++
    dateFormat.map("logger.json.dateformat" -> _).toSet

  override def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {

    val hc =
      HeaderCarrierConverter.fromRequestAndSession(rh, rh.session)

    val data = Set(
      hc.requestId.map(HeaderNames.xRequestId    -> _.value),
      hc.sessionId.map(HeaderNames.xSessionId    -> _.value),
      hc.forwarded.map(HeaderNames.xForwardedFor -> _.value)
    ).flatten ++ extras

    data.foreach {
      case (k, v) =>
        MDC.put(k, v)
    }

    f(rh)
  }
}
