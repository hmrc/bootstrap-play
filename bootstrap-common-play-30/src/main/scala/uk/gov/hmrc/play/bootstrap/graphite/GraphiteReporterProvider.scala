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

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Provider}

import com.codahale.metrics.MetricFilter
import com.codahale.metrics.graphite.{Graphite, GraphiteReporter}
import uk.gov.hmrc.play.bootstrap.metrics.Metrics
import play.api.Configuration

case class GraphiteReporterProviderConfig(
  prefix   : String,
  rates    : TimeUnit,
  durations: TimeUnit
)

object GraphiteReporterProviderConfig {
  def fromConfig(config: Configuration): GraphiteReporterProviderConfig =
    GraphiteReporterProviderConfig(
      prefix    = config.get[String]("microservice.metrics.graphite.prefix"),
      rates     = TimeUnit.valueOf(config.get[String]("microservice.metrics.graphite.rates")),
      durations = TimeUnit.valueOf(config.get[String]("microservice.metrics.graphite.durations"))
    )
}

class GraphiteReporterProvider @Inject()(
  config  : GraphiteReporterProviderConfig,
  metrics : Metrics,
  graphite: Graphite,
  filter  : MetricFilter
) extends Provider[GraphiteReporter] {

  override def get(): GraphiteReporter =
    GraphiteReporter
      .forRegistry(metrics.defaultRegistry)
      .prefixedWith(s"${config.prefix}.${java.net.InetAddress.getLocalHost.getHostName}")
      .convertDurationsTo(config.durations)
      .convertRatesTo(config.rates)
      .filter(filter)
      .build(graphite)
}
