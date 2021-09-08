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
  prefix   : String,
  rates    : Option[TimeUnit],
  durations: Option[TimeUnit]
)

object GraphiteReporterProviderConfig {
  def fromConfig(config: Configuration): GraphiteReporterProviderConfig = {
    lazy val appName = config.getOptional[String]("appName")

    val prefix: String = config
      .getOptional[String]("microservice.metrics.graphite.prefix")
      .orElse(appName.map(name => s"tax.$name"))
      .getOrElse(
        throw new ConfigException.Generic("`microservice.metrics.graphite.prefix` in config or `appName` as parameter required"))

    GraphiteReporterProviderConfig(
      prefix    = prefix,
      rates     = config.getOptional[String]("microservice.metrics.graphite.rates").map(TimeUnit.valueOf),
      durations = config.getOptional[String]("microservice.metrics.graphite.durations").map(TimeUnit.valueOf)
    )
  }
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
      .convertDurationsTo(config.durations.getOrElse(TimeUnit.SECONDS))
      .convertRatesTo(config.rates.getOrElse(TimeUnit.MILLISECONDS))
      .filter(filter)
      .build(graphite)
}
