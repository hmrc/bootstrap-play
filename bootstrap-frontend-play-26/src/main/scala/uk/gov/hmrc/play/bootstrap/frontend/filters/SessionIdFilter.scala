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

import akka.stream.Materializer
import com.google.inject.Inject
import play.api.mvc._
import play.api.mvc.request.{Cell, RequestAttrKey}
import uk.gov.hmrc.http.{SessionKeys, HeaderNames => HMRCHeaderNames}

import scala.concurrent.{ExecutionContext, Future}

class SessionIdFilter(
  override val mat: Materializer,
  uuid: => UUID,
  sessionCookieBaker: SessionCookieBaker,
  implicit val ec: ExecutionContext
) extends Filter {

  @Inject
  def this(mat: Materializer, ec: ExecutionContext, sessionCookieBaker: SessionCookieBaker) {
    this(mat, UUID.randomUUID(), sessionCookieBaker, ec)
  }

  override def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {

    if (rh.session.get(SessionKeys.sessionId).isEmpty) {

      val rhWithSessionId = {
        val sessionId = s"session-$uuid"
        val headers = rh.headers.add(HMRCHeaderNames.xSessionId -> sessionId)
        val session = rh.session + (SessionKeys.sessionId -> sessionId)
        rh.withHeaders(headers).addAttr(RequestAttrKey.Session, Cell(session))
      }

      f(rhWithSessionId)
        .map(result => result.withSession(session = result.session(rhWithSessionId)))

    } else {
      f(rh)
    }
  }
}
