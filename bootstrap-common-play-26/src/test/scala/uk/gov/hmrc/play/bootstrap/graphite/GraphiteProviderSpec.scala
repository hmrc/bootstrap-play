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
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers

class GraphiteProviderSpec extends AnyWordSpec with Matchers {

  "GraphiteProviderConfigSpec.fromConfig" must {

    val configuration: Map[String, String] = Map(
      "host" -> "localhost",
      "port" -> "9999"
    )

    "return a valid `GraphiteProviderConfig`" in {
      val app = new GuiceApplicationBuilder()
        .configure(configuration)
        .build()

      Helpers.running(app) {
        val config: GraphiteProviderConfig =
          GraphiteProviderConfig.fromConfig(app.injector.instanceOf[Configuration])

        config.host mustEqual "localhost"
        config.port mustEqual 9999
      }
    }
    configuration.keys.foreach { key =>
      s"throw a configuration exception when config key: $key, is missing" in {

        val app = new GuiceApplicationBuilder()
          .configure(configuration - key)
          .build()

        Helpers.running(app) {
          intercept[ConfigException.Missing] {
            GraphiteProviderConfig.fromConfig(app.injector.instanceOf[Configuration])
          }
        }
      }
    }
  }
}
