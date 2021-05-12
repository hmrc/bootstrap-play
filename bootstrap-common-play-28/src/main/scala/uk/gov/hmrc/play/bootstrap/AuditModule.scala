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

package uk.gov.hmrc.play.bootstrap

import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.audit.http.connector.{AuditChannel, AuditConnector, AuditCounter, AuditCounterLogs, AuditCounterMetrics}
import uk.gov.hmrc.play.bootstrap.audit.{DefaultAuditChannel, DefaultAuditConnector, DefaultAuditCounter, DefaultAuditCounterLogs, DefaultAuditCounterMetrics}

class AuditModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = Seq(
    bind[AuditChannel].to[DefaultAuditChannel],
    bind[AuditCounterMetrics].to[DefaultAuditCounterMetrics],
    bind[AuditCounterLogs].to[DefaultAuditCounterLogs],
    bind[AuditCounter].to[DefaultAuditCounter],
    bind[AuditConnector].to[DefaultAuditConnector]
  )
}