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

import com.typesafe.config.ConfigException
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration

class GraphiteReporterProviderConfigSpec extends AnyWordSpec with Matchers {

  "GraphiteReporterProviderConfig.fromConfig" should {
    "return a valid `GraphiteReporterProviderConfig` when given config" in {
      val config = Configuration(
        "microservice.metrics.graphite.prefix"    -> "test",
        "microservice.metrics.graphite.durations" -> "SECONDS",
        "microservice.metrics.graphite.rates"     -> "SECONDS"
      )

      GraphiteReporterProviderConfig.fromConfig(config) shouldBe GraphiteReporterProviderConfig(
        prefix    = "test",
        rates     = TimeUnit.SECONDS,
        durations = TimeUnit.SECONDS
      )
    }

    "throw a configuration exception when relevant keys are missing" in {
      an [ConfigException.Missing] should be thrownBy GraphiteReporterProviderConfig.fromConfig(Configuration())
    }
  }
}
