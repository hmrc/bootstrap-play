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

package uk.gov.hmrc.play.bootstrap.audit

import uk.gov.hmrc.play.bootstrap.metrics.Metrics
import org.slf4j.LoggerFactory
import uk.gov.hmrc.play.audit.http.connector.{Counter, DatastreamMetrics}
import uk.gov.hmrc.play.bootstrap.graphite.GraphiteReporterProviderConfig

import javax.inject.{Inject, Provider, Singleton}

@Singleton
class EnabledDatastreamMetricsProvider @Inject()(
  reporter: GraphiteReporterProviderConfig,
  metrics : Metrics
) extends Provider[DatastreamMetrics] {
  private lazy val datastreamMetrics =
    DatastreamMetrics(
      prefix    = reporter.prefix,
      mkCounter = (name: String) =>
                    new Counter {
                      private val counter = metrics.defaultRegistry.counter(name)
                      def inc() = counter.inc()
                    }
    )
  override def get(): DatastreamMetrics =
    datastreamMetrics
}

@Singleton
class DisabledDatastreamMetricsProvider extends Provider[DatastreamMetrics] {
  private[audit] val logger = LoggerFactory.getLogger(getClass)
  private lazy val datastreamMetrics = {
    logger.warn("DatastreamMetrics have been disabled since metrics are disabled: ENABLE IN ALL TESTING + PRODUCTION ENVIRONMENTS")
    DatastreamMetrics.disabled
  }

  override def get(): DatastreamMetrics =
    datastreamMetrics
}
