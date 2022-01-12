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
import play.api.mvc.{RequestHeader, ResponseHeader, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider
import uk.gov.hmrc.play.bootstrap.config.{ControllerConfigs, HttpAuditEvent}
import uk.gov.hmrc.play.bootstrap.filters.CommonAuditFilter

import scala.concurrent.ExecutionContext

trait BackendAuditFilter
  extends CommonAuditFilter
     with BackendHeaderCarrierProvider {

  override protected def filterResponseBody(result: Result, response: ResponseHeader, responseBody: String): String =
    responseBody

  override protected def buildRequestDetails(requestHeader: RequestHeader, request: String): Map[String, String] =
    Map.empty

  override protected def buildResponseDetails(response: ResponseHeader): Map[String, String] =
    Map.empty
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

  override def dataEvent(
    eventType      : String,
    transactionName: String,
    request        : RequestHeader,
    detail         : Map[String, String]
  )(implicit hc: HeaderCarrier): DataEvent =
    httpAuditEvent.dataEvent(eventType, transactionName, request, detail)
}
