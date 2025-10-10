/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.play.bootstrap.backend.filters.session

import org.apache.pekko.stream.Materializer
import play.api.Logger
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

/**
 * Detect session data in backend services where sessions are unexpected.
 * If any session data is detected, a warning will be logged.
 */
trait BackendSessionLoggingFilter extends Filter {

  protected implicit def ec: ExecutionContext

  protected val logger = Logger(getClass)

  override def apply(next: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
    if (rh.session.data.nonEmpty)
      logger.warn(s"Session data detected in incoming request to ${rh.method} ${rh.uri}. Session data should not be sent to backend services.")

    next(rh)
  }
}

class DefaultBackendSessionLoggingFilter @Inject()()(
  implicit
  override val mat: Materializer,
  override val ec: ExecutionContext
) extends BackendSessionLoggingFilter
