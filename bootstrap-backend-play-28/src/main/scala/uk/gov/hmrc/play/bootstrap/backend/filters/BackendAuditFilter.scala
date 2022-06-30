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

package uk.gov.hmrc.play.bootstrap.backend.filters

import akka.stream.Materializer

import javax.inject.Inject
import play.api.Configuration
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{RequestHeader, ResponseHeader}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.hooks.Body
import uk.gov.hmrc.play.audit.EventKeys
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{ExtendedDataEvent, RedactionLog, TruncationLog}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider
import uk.gov.hmrc.play.bootstrap.config.{ControllerConfigs, HttpAuditEvent}
import uk.gov.hmrc.play.bootstrap.filters.{CommonAuditFilter, Details}

import scala.concurrent.ExecutionContext

trait BackendAuditFilter
  extends CommonAuditFilter
     with BackendHeaderCarrierProvider {

  override protected def buildRequestDetails(requestHeader: RequestHeader, requestBody: Body[String]): Details =
    Details.empty

  override protected def buildResponseDetails(responseHeader: ResponseHeader, responseBody: Body[String], contentType: Option[String]): Details = {
    val (responseBodyStr, isResponseTruncated) = responseBody match {
      case Body.Complete(b)  => (b, false)
      case Body.Truncated(b) => (b, true)
    }

    val responseDetails =
      Json.obj(
        EventKeys.StatusCode      -> responseHeader.status.toString,
        EventKeys.ResponseMessage -> responseBodyStr
       )

    val truncationLog =
      TruncationLog.of(truncatedFields = if (isResponseTruncated) List(EventKeys.ResponseMessage) else List.empty)

    Details(responseDetails, truncationLog, RedactionLog.Empty)
  }
}

class DefaultBackendAuditFilter @Inject()(
  override val config: Configuration,
  controllerConfigs: ControllerConfigs,
  override val auditConnector: AuditConnector,
  httpAuditEvent: HttpAuditEvent,
  override val mat: Materializer
)(implicit protected val ec: ExecutionContext
) extends BackendAuditFilter {

  override def controllerNeedsAuditing(controllerName: String): Boolean =
    controllerConfigs.controllerNeedsAuditing(controllerName)

  override def extendedDataEvent(
    eventType      : String,
    transactionName: String,
    request        : RequestHeader,
    detail         : JsObject,
    truncationLog  : TruncationLog,
    redactionLog   : RedactionLog
  )(implicit hc: HeaderCarrier): ExtendedDataEvent =
    httpAuditEvent.extendedEvent(
      eventType,
      transactionName,
      request,
      detail,
      truncationLog,
      redactionLog
    )
}
