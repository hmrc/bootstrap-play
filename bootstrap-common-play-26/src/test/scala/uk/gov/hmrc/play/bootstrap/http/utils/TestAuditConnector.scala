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

package uk.gov.hmrc.play.bootstrap.http.utils

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import play.api.inject.{ApplicationLifecycle, DefaultApplicationLifecycle}
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.http.connector.{AuditChannel, AuditConnector, AuditCounter, AuditCounterMetrics}

import scala.concurrent.ExecutionContext

class TestAuditConnector(appName: String) extends AuditConnector {
  private val config = AuditingConfig(
    consumer         = None,
    enabled          = false,
    auditSource      = appName,
    auditSentHeaders = false
  )
  private val _actorSystem = ActorSystem()
  private val _materializer = ActorMaterializer()(_actorSystem)
  private val _applicationLifecycle = new DefaultApplicationLifecycle()
  private val _auditChannel = new AuditChannel {
    override def auditingConfig: AuditingConfig = config
    override def materializer  : Materializer = _materializer
    override def lifecycle: ApplicationLifecycle = _applicationLifecycle
  }
  override val auditingConfig: AuditingConfig = config
  override def materializer: Materializer = _materializer
  override def lifecycle: ApplicationLifecycle = _applicationLifecycle
  override def auditChannel  : AuditChannel = _auditChannel
  override def auditCounter  : AuditCounter = new AuditCounter {
    def actorSystem: ActorSystem = _actorSystem
    def auditingConfig: AuditingConfig = config
    def lifecycle: ApplicationLifecycle = _applicationLifecycle
    def ec: ExecutionContext = ???
    def auditChannel: AuditChannel = _auditChannel
    def auditMetrics: AuditCounterMetrics = new AuditCounterMetrics {
      def registerMetric(name:String, read:()=>Long):Unit = {}
    }
  }
}
