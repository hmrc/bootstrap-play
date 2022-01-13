/*
 * Copyright 2022 HM Revenue & Customs
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
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration

class GraphiteReporterProviderConfigSpec extends AnyWordSpec with Matchers {

  "GraphiteReporterProviderConfig.fromConfig" should {
    "return a valid `GraphiteReporterProviderConfig` when given a prefix" in {
      val config = Configuration(
        "microservice.metrics.graphite.prefix" -> "test",
        "appName"                              -> "testApp"
      )

      GraphiteReporterProviderConfig.fromConfig(config) shouldBe GraphiteReporterProviderConfig(
        prefix    = "test",
        rates     = None,
        durations = None
      )
    }

    "return a valid `GraphiteReporterProviderConfig` when given a prefix and optional config" in {
      val config = Configuration(
        "microservice.metrics.graphite.prefix"    -> "test",
        "microservice.metrics.graphite.durations" -> "SECONDS",
        "microservice.metrics.graphite.rates"     -> "SECONDS"
      )

      GraphiteReporterProviderConfig.fromConfig(config) shouldBe GraphiteReporterProviderConfig(
        prefix    = "test",
        rates     = Some(TimeUnit.SECONDS),
        durations = Some(TimeUnit.SECONDS)
      )
    }

    "return a valid `GraphiteReporterProviderConfig` when given an appName" in {
      val config = Configuration("appName" -> "testApp")

      GraphiteReporterProviderConfig.fromConfig(config) shouldBe GraphiteReporterProviderConfig(
        prefix    = "tax.testApp",
        rates     = None,
        durations = None
      )
    }

    "throw a configuration exception when relevant keys are missing" in {
      val exception = intercept[ConfigException.Generic] {
        GraphiteReporterProviderConfig.fromConfig(Configuration())
      }

      exception.getMessage shouldEqual "`microservice.metrics.graphite.prefix` in config or `appName` as parameter required"
    }
  }
}
