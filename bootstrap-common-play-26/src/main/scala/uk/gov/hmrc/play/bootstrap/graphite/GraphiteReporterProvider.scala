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

package uk.gov.hmrc.play.bootstrap.graphite

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Provider}

import com.codahale.metrics.MetricFilter
import com.codahale.metrics.graphite.{Graphite, GraphiteReporter}
import com.kenshoo.play.metrics.Metrics
import com.typesafe.config.ConfigException
import play.api.Configuration

case class GraphiteReporterProviderConfig(
  prefix: String,
  rates: Option[TimeUnit],
  durations: Option[TimeUnit]
)

object GraphiteReporterProviderConfig {

  def fromConfig(config: Configuration, graphiteConfiguration: Configuration): GraphiteReporterProviderConfig = {

    val appName: Option[String]     = config.getOptional[String]("appName")
    val rates: Option[TimeUnit]     = graphiteConfiguration.getOptional[String]("rates").map(TimeUnit.valueOf)
    val durations: Option[TimeUnit] = graphiteConfiguration.getOptional[String]("durations").map(TimeUnit.valueOf)

    val prefix: String = graphiteConfiguration
      .getOptional[String]("prefix")
      .orElse(appName.map(name => s"tax.$name"))
      .getOrElse(
        throw new ConfigException.Generic("`metrics.graphite.prefix` in config or `appName` as parameter required"))

    GraphiteReporterProviderConfig(prefix, rates, durations)
  }
}

class GraphiteReporterProvider @Inject()(
  config: GraphiteReporterProviderConfig,
  metrics: Metrics,
  graphite: Graphite,
  filter: MetricFilter
) extends Provider[GraphiteReporter] {

  override def get(): GraphiteReporter =
    GraphiteReporter
      .forRegistry(metrics.defaultRegistry)
      .prefixedWith(s"${config.prefix}.${java.net.InetAddress.getLocalHost.getHostName}")
      .convertDurationsTo(config.durations.getOrElse(TimeUnit.SECONDS))
      .convertRatesTo(config.rates.getOrElse(TimeUnit.MILLISECONDS))
      .filter(filter)
      .build(graphite)
}
