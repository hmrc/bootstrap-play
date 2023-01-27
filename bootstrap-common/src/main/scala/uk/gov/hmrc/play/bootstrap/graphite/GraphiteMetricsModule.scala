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

package uk.gov.hmrc.play.bootstrap.graphite

import com.codahale.metrics.{Metric, MetricFilter, MetricRegistry, MetricSet}
import com.codahale.metrics.graphite.{Graphite, GraphiteReporter}
import com.kenshoo.play.metrics.{DisabledMetricsFilter, Metrics, MetricsImpl, MetricsFilter, MetricsFilterImpl}
import javax.inject.Singleton
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.audit.http.connector.DatastreamMetrics
import uk.gov.hmrc.play.bootstrap.audit.{DisabledDatastreamMetricsProvider, EnabledDatastreamMetricsProvider}

class GraphiteMetricsModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    val defaultBindings: Seq[Binding[_]] = Seq(
      // Note: `MetricFilter` rather than `MetricsFilter`
      bind[MetricFilter].toInstance(MetricFilter.ALL).eagerly()
    )

    val kenshooMetricsEnabled    = configuration.get[Boolean]("metrics.enabled") // metrics collection
    val graphitePublisherEnabled = configuration.get[Boolean]("microservice.metrics.graphite.enabled") // metrics publishing

    val kenshooBindings: Seq[Binding[_]] =
      if (kenshooMetricsEnabled)
        Seq(
          bind[MetricsFilter].to[MetricsFilterImpl].eagerly(),
          bind[Metrics].to[MetricsImpl].eagerly()
        )
      else
        Seq(
          bind[MetricsFilter].to[DisabledMetricsFilter].eagerly(),
          bind[Metrics].to[DisabledMetrics].eagerly()
        )

    val graphiteBindings: Seq[Binding[_]] =
      if (kenshooMetricsEnabled && graphitePublisherEnabled)
        Seq(
          bind[GraphiteProviderConfig].toInstance(GraphiteProviderConfig.fromRootConfig(configuration)),
          bind[GraphiteReporterProviderConfig].toInstance(GraphiteReporterProviderConfig.fromConfig(configuration)),
          bind[Graphite].toProvider[GraphiteProvider],
          bind[GraphiteReporter].toProvider[GraphiteReporterProvider],
          bind[DatastreamMetrics].toProvider[EnabledDatastreamMetricsProvider],
          bind[GraphiteReporting].to[EnabledGraphiteReporting].eagerly()
        )
      else
        Seq(
          bind[DatastreamMetrics].toProvider[DisabledDatastreamMetricsProvider],
          bind[GraphiteReporting].to[DisabledGraphiteReporting].eagerly()
        )

    defaultBindings ++ graphiteBindings ++ kenshooBindings
  }
}

/** An alternative to com.kenshoo.play.metrics.DisabledMetrics, this implementation
  * will not generate `MetricsDisabledException` if the injected metrics is used
  * (we do not expect clients to check metrics.enabled before using the injected metrics)
  */
@Singleton
class DisabledMetrics extends Metrics {
  override def defaultRegistry: MetricRegistry = new MetricRegistry {
    override def register[T <: Metric](name: String, metric: T): T = metric
    override def registerAll(metrics: MetricSet): Unit = ()
  }

  override def toJson: String = "null"
}
