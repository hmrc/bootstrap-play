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

package uk.gov.hmrc.play.bootstrap.audit

import akka.actor.{ActorSystem, CoordinatedShutdown}
import com.codahale.metrics.Gauge
import com.codahale.metrics.MetricRegistry.MetricSupplier
import com.kenshoo.play.metrics.Metrics
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.http.connector.{AuditChannel, AuditCounter, AuditCounterMetrics}

import scala.concurrent.ExecutionContext

@Singleton
class DefaultAuditCounter @Inject()(
  val actorSystem: ActorSystem,
  val auditingConfig: AuditingConfig,
  val coordinatedShutdown : CoordinatedShutdown,
  val ec : ExecutionContext,
  val auditChannel:AuditChannel,
  metrics: Metrics
) extends AuditCounter {
  lazy val auditMetrics = new AuditCounterMetrics {
    def registerMetric(name:String, read:()=>Long):Unit = {
      metrics.defaultRegistry.gauge(name, new MetricSupplier[Gauge[_]] {
        override def newMetric(): Gauge[_] = new Gauge[Long] {
          override def getValue: Long = read()
        }
      })
    }
  }
}
