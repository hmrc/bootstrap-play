/*
 * Copyright 2020 HM Revenue & Customs
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

import com.codahale.metrics.graphite.Graphite
import com.typesafe.config.Config
import javax.inject.{Inject, Provider}
import play.api.Configuration

case class GraphiteProviderConfig(
  host: String,
  port: Int
)

object GraphiteProviderConfig {

  def fromConfig(graphiteConfiguration: Configuration): GraphiteProviderConfig = {

    val graphite: Config = graphiteConfiguration.underlying
    val host: String     = graphite.getString("host")
    val port: Int        = graphite.getInt("port")

    GraphiteProviderConfig(host, port)
  }
}

class GraphiteProvider @Inject()(
  config: GraphiteProviderConfig
) extends Provider[Graphite] {

  override def get(): Graphite =
    new Graphite(config.host, config.port)
}
