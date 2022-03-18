/*
 * Copyright 2022 HM Revenue & Customs
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
import javax.inject.Inject
import play.api.mvc.{Filter, RequestHeader, Result}
import play.api.routing.Router.Attrs
import play.api.{Logger, LoggerLike}
import uk.gov.hmrc.play.bootstrap.config.ControllerConfigs

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

trait LoggingFilter extends Filter {
  implicit val ec: ExecutionContext

  def controllerNeedsLogging(controllerName: String): Boolean

  val now: () => Long =
    () => System.currentTimeMillis()

  protected def logger: LoggerLike = Logger(getClass)

  def apply(next: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    val startTime   = now()
    val result      = next(rh)

    if (needsLogging(rh))
      log(rh, result, startTime)
    else
      result
  }

  private def needsLogging(request: RequestHeader): Boolean =
    request.attrs.get(Attrs.HandlerDef).forall { handlerDef =>
      controllerNeedsLogging(handlerDef.controller)
    }

  private def log(rh: RequestHeader, resultF: Future[Result], startTime: Long): Future[Result] = {

    def elapsedTime = now() - startTime

    resultF
      .andThen {
        case Success(result) =>
          logger.info(
            s"${rh.method} ${rh.uri} ${result.header.status} ${elapsedTime}ms"
          )

        case Failure(NonFatal(t)) =>
          logger.info(
            s"${rh.method} ${rh.uri} $t ${elapsedTime}ms"
          )
      }
  }
}

class DefaultLoggingFilter @Inject()(config: ControllerConfigs)(implicit override val mat: Materializer, val ec: ExecutionContext)
    extends LoggingFilter {

  override def controllerNeedsLogging(controllerName: String): Boolean =
    config.get(controllerName).logging
}
