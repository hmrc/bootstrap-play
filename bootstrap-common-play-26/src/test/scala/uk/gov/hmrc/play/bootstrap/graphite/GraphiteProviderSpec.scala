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

import com.typesafe.config.ConfigException
import org.scalatest.{MustMatchers, WordSpec}
import play.api.Configuration
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder

class GraphiteProviderSpec extends WordSpec with MustMatchers {

  "GraphiteProviderConfigSpec.fromConfig" must {

    val configuration: Map[String, String] = Map(
      "host" -> "localhost",
      "port" -> "9999"
    )

    "return a valid `GraphiteProviderConfig`" in {

      val injector: Injector = new GuiceApplicationBuilder()
        .configure(configuration)
        .build()
        .injector

      val config: GraphiteProviderConfig =
        GraphiteProviderConfig.fromConfig(injector.instanceOf[Configuration])

      config.host mustEqual "localhost"
      config.port mustEqual 9999
    }

    configuration.keys.foreach { key =>
      s"throw a configuration exception when config key: $key, is missing" in {

        val injector = new GuiceApplicationBuilder()
          .configure(configuration - key)
          .build()
          .injector

        intercept[ConfigException.Missing] {
          GraphiteProviderConfig.fromConfig(injector.instanceOf[Configuration])
        }
      }
    }
  }
}
