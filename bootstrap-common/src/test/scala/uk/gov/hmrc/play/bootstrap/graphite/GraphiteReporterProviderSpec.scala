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

import com.typesafe.config.ConfigException
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration

class GraphiteReporterProviderSpec extends AnyWordSpec with Matchers {

  "GraphiteReporterProviderConfig.fromConfig" must {

    "return a valid `GraphiteReporterProviderConfig` when given a prefix" in {

      val rootConfig = Configuration(
        "appName"                              -> "testApp",
        "microservice.metrics.graphite.prefix" -> "test"
      )

      val graphiteConfig = rootConfig.get[Configuration]("microservice.metrics.graphite")

      val config: GraphiteReporterProviderConfig =
        GraphiteReporterProviderConfig.fromConfig(rootConfig, graphiteConfig)

      config.prefix mustEqual "test"
      config.rates mustBe None
      config.durations mustBe None
    }

    "return a valid `GraphiteReporterProviderConfig` when given a prefix and optional config" in {

      val rootConfig = Configuration(
        "microservice.metrics.graphite.prefix"    -> "test",
        "microservice.metrics.graphite.durations" -> "SECONDS",
        "microservice.metrics.graphite.rates"     -> "SECONDS"
      )
      val graphiteConfig = rootConfig.get[Configuration]("microservice.metrics.graphite")

      val config: GraphiteReporterProviderConfig =
        GraphiteReporterProviderConfig.fromConfig(rootConfig, graphiteConfig)

      config.prefix mustEqual "test"
      config.rates mustBe Some(TimeUnit.SECONDS)
      config.durations mustBe Some(TimeUnit.SECONDS)
    }

    "return a valid `GraphiteReporterProviderConfig` when given an appName" in {

      val rootConfig     = Configuration("appName" -> "testApp")
      val graphiteConfig = Configuration()

      val config: GraphiteReporterProviderConfig =
        GraphiteReporterProviderConfig.fromConfig(rootConfig, graphiteConfig)

      config.prefix mustEqual "tax.testApp"
      config.rates mustBe None
      config.durations mustBe None
    }

    "throw a configuration exception when relevant keys are missing" in {

      val exception = intercept[ConfigException.Generic] {
        GraphiteReporterProviderConfig.fromConfig(Configuration(), Configuration())
      }

      exception.getMessage mustEqual "`metrics.graphite.prefix` in config or `appName` as parameter required"
    }
  }
}
