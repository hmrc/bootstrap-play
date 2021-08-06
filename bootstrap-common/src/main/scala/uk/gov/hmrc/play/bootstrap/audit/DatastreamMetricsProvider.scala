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

import com.codahale.metrics.{Counter => CodahaleCounter}
import com.kenshoo.play.metrics.Metrics
import org.slf4j.LoggerFactory
import uk.gov.hmrc.play.audit.http.connector.{Counter, DatastreamMetrics}
import uk.gov.hmrc.play.bootstrap.graphite.GraphiteReporterProviderConfig

import javax.inject.{Inject, Provider}


class EnabledCounter(counter: CodahaleCounter) extends Counter {
  def inc() = counter.inc()
}
class EnabledDatastreamMetricsProvider @Inject()(reporter: GraphiteReporterProviderConfig, metrics: Metrics) extends Provider[DatastreamMetrics] {
  def get() = DatastreamMetrics(
    successCounter = new EnabledCounter(metrics.defaultRegistry.counter("audit.success")),
    rejectCounter = new EnabledCounter(metrics.defaultRegistry.counter("audit.reject")),
    failureCounter = new EnabledCounter(metrics.defaultRegistry.counter("audit.failure")),
    metricsKey = Some(reporter.prefix)
  )
}


case object DisabledCounter extends Counter {
  def inc() = ()
}

class DisabledDatastreamMetricsProvider extends Provider[DatastreamMetrics] {
  val logger = LoggerFactory.getLogger(getClass)

  def get() = {
    logger.warn("DatastreamMetrics have been disabled since metrics are disabled: ENABLE IN ALL TESTING + PRODUCTION ENVIRONMENTS")
    DatastreamMetrics(
      DisabledCounter, DisabledCounter, DisabledCounter, None
    )
  }
}