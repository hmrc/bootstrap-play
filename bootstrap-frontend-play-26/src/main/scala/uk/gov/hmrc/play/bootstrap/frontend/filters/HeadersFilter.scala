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

package uk.gov.hmrc.play.bootstrap.frontend.filters

import java.util.UUID

import javax.inject.Inject
import akka.stream.Materializer
import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.http.HeaderNames.{xRequestId, xRequestTimestamp}

import scala.concurrent.Future

class HeadersFilter @Inject()(override val mat: Materializer) extends Filter {

  private val logger = Logger(this.getClass)

  override def apply(next: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {

    val request = rh.session.get(xRequestId) match {
      case Some(_) =>
        // BDOG-836: safety check monitoring prior to removal of (what we believe is) an unnecessary session lookup
        logger.warn(s"Not allocating an $xRequestId header as one unexpectedly exists in the session")
        rh
      case None =>
        rh.withHeaders(rh.headers.add(newHeaders: _*))
    }

    next(request)
  }

  protected def newHeaders: Seq[(String, String)] = {

    val rid = s"govuk-tax-${UUID.randomUUID().toString}"

    Seq(
      xRequestId        -> rid,
      xRequestTimestamp -> System.nanoTime().toString
    )
  }
}
