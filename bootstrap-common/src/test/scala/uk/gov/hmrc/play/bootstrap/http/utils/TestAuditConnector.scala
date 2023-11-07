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

package uk.gov.hmrc.play.bootstrap.http.utils

import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.http.connector.{AuditChannel, AuditConnector, DatastreamMetrics}
import uk.gov.hmrc.play.bootstrap.stream.Materializer

import javax.inject.{Inject, Named}

class TestAuditConnector @Inject() (
  @Named("appName") appName: String,
  lifecycle: ApplicationLifecycle,
  mat: Materializer
) extends AuditConnector { outer =>

  private val _auditingConfig = AuditingConfig(
    consumer         = None,
    enabled          = false,
    auditSource      = appName,
    auditSentHeaders = false
  )

  override val auditingConfig: AuditingConfig = _auditingConfig

  override def datastreamMetrics: DatastreamMetrics = ???

  override val auditChannel: AuditChannel = new AuditChannel {
    override val auditingConfig   : AuditingConfig       = _auditingConfig
    override val materializer     : Materializer         = mat
    override val lifecycle        : ApplicationLifecycle = outer.lifecycle
    override def datastreamMetrics: DatastreamMetrics    = ???
  }
}
