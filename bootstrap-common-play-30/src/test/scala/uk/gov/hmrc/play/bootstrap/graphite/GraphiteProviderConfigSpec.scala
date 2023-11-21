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

import com.typesafe.config.ConfigException
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration

class GraphiteProviderConfigSpec extends AnyWordSpec with Matchers {

  "GraphiteProviderConfig.fromConfig" should {
    val configuration = Map(
      "microservice.metrics.graphite.host" -> "localhost",
      "microservice.metrics.graphite.port" -> "9999"
    )

    "return a valid `GraphiteProviderConfig`" in {
      GraphiteProviderConfig.fromRootConfig(Configuration.from(configuration)) shouldBe GraphiteProviderConfig(
        host = "localhost",
        port = 9999
      )
    }
    configuration.keys.foreach { key =>
      s"throw a configuration exception when config key: $key, is missing" in {
        intercept[ConfigException.Missing] {
          GraphiteProviderConfig.fromRootConfig(Configuration.from(configuration - key))
        }
      }
    }
  }
}
