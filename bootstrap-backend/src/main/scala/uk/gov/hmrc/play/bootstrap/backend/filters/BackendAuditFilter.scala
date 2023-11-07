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

package uk.gov.hmrc.play.bootstrap.backend.filters


import javax.inject.Inject
import play.api.Configuration
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{RequestHeader, ResponseHeader}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.hooks.Data
import uk.gov.hmrc.play.audit.EventKeys
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{ExtendedDataEvent, RedactionLog, TruncationLog}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider
import uk.gov.hmrc.play.bootstrap.config.{ControllerConfigs, HttpAuditEvent}
import uk.gov.hmrc.play.bootstrap.filters.{CommonAuditFilter, Details}
import uk.gov.hmrc.play.bootstrap.stream.Materializer

import scala.concurrent.ExecutionContext

trait BackendAuditFilter
  extends CommonAuditFilter
     with BackendHeaderCarrierProvider {

  override protected def buildRequestDetails(requestHeader: RequestHeader, requestBody: Data[String]): Details =
    Details.empty

  override protected def buildResponseDetails(responseHeader: ResponseHeader, responseBody: Data[String], contentType: Option[String]): Details = {
    val responseDetails =
      Json.obj(
        EventKeys.StatusCode      -> responseHeader.status.toString,
        EventKeys.ResponseMessage -> responseBody.value
       )

    val truncationLog =
      if (responseBody.isTruncated)
        TruncationLog.Entry(List(EventKeys.ResponseMessage))
      else
        TruncationLog.Empty

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
