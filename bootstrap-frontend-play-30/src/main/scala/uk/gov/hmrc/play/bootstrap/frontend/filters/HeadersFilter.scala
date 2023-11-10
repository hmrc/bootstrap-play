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

package uk.gov.hmrc.play.bootstrap.frontend.filters

import javax.inject.Inject
import org.apache.pekko.stream.Materializer
import play.api.mvc._
import uk.gov.hmrc.http.HeaderNames

import java.util.UUID
import scala.concurrent.Future

class HeadersFilter @Inject()(override val mat: Materializer) extends Filter {

  override def apply(next: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
    def ifMissing(header: String, value: => String): Seq[(String, String)] =
      if (!rh.headers.hasHeader(header)) Seq(header -> value) else Seq.empty

    val newHeaders =
      ifMissing(HeaderNames.xRequestId       , s"govuk-tax-${UUID.randomUUID().toString}") ++
      ifMissing(HeaderNames.xRequestTimestamp, System.nanoTime().toString)

    next(rh.withHeaders(rh.headers.add(newHeaders: _*)))
  }
}
