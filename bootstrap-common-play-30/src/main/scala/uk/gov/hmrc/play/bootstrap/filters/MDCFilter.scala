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
import play.api.{Configuration, Logger}
import play.api.mvc.{Filter, RequestHeader, Result}
import play.api.routing.{HandlerDef, Router}
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}
import uk.gov.hmrc.mdc.{Mdc, RequestMdc}

import scala.concurrent.{ExecutionContext, Future}

trait MDCFilter extends Filter {

  private val logger = Logger(getClass)

  val mat: Materializer
  val configuration: Configuration
  implicit val ec: ExecutionContext

  private val isMdcTrackingEnabled: Boolean =
    configuration.get[Boolean]("bootstrap.mdc.tracking.enabled")

  private val mdcTrackingWarnThresholdPercent: Int =
    configuration.get[Int]("bootstrap.mdc.tracking.warnThresholdPercent")

  protected def hc(implicit rh: RequestHeader): HeaderCarrier

  override def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
    val headerCarrier = hc(rh)
    val data = Map(
      HeaderNames.xRequestId    -> headerCarrier.requestId.fold("-")(_.value),
      HeaderNames.xSessionId    -> headerCarrier.sessionId.fold("-")(_.value),
      HeaderNames.xForwardedFor -> headerCarrier.forwarded.fold("-")(_.value)
    )
    RequestMdc.add(rh.id, data)

    f(rh)
      .andThen { _ =>
        if (isMdcTrackingEnabled) {
          import Router.RequestImplicits._
          rh.handlerDef match {
            case Some(handlerDef) => trackMdc(handlerDef, data)
            case _                => // 404s for example will not have a HandlerDef - we don't want to track these anyway
          }
        }
        // we do not clean up MDC here, since it would _not_ be available for the ErrorHandler
      }
  }

  // Tracks (lossCount, totalCount) for each request (verb/controller method).
  // The Map key is limited to the number of routes, so we don't need to use a
  // WeakHashMap for example (unlike request.path which would be subject to any parameter permutation)
  private val mdcStatus =
    new java.util.concurrent.atomic.AtomicReference(
      Map.empty[String, (Long, Long)]
    )

  def resetMdcTracking(): Unit = {
    if (!isMdcTrackingEnabled)
      throw new IllegalStateException("MDC tracking is not enabled")
    mdcStatus.set(Map.empty)
  }

  def isMdcLost(): Boolean = {
    if (!isMdcTrackingEnabled)
      throw new IllegalStateException("MDC tracking is not enabled")
    !mdcStatus.get.forall(_._2._1 == 0)
  }

  private def trackMdc(handlerDef: HandlerDef, data: Map[String, String]): Unit = {
    val request =
      s"${handlerDef.verb} ${handlerDef.controller}.${handlerDef.method}"

    val isMdcPreserved = {
      val mdcData = Mdc.mdcData.toSet
      data.forall(mdcData.contains)
    }

    val m =
      mdcStatus.updateAndGet { m =>
        val (loss   , total   ) = m.getOrElse(request, (0L, 0L))
        val (newLoss, newTotal) = (loss + (if (isMdcPreserved) 0L else 1L), total + 1L)
        m.concat(Map(request -> (newLoss, newTotal)))
      }
    if (!isMdcPreserved) {
      val (loss, total) = m(request)
      val p = BigDecimal(100 * loss / total).setScale(0, BigDecimal.RoundingMode.HALF_UP)
      if (p > mdcTrackingWarnThresholdPercent)
        logger.warn(s"MDC Data has been dropped for endpoint: $request. (For this instance: loss/total = $loss/$total - i.e. ${p}%)")
    }
  }
}
