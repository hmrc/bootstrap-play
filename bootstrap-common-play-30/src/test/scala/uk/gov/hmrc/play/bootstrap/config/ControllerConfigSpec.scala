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

package uk.gov.hmrc.play.bootstrap.config

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration

class ControllerConfigSpec extends AnyWordSpec with Matchers {

  "ControllerConfig.fromConfig" should {
    "return defaults when no config is found" in {

      val config = ControllerConfig.fromConfig(Configuration())

      config.auditing shouldBe true
      config.logging  shouldBe true
    }

    "return defaults when config is set to defaults" in {
      val config = ControllerConfig.fromConfig(Configuration("needsAuditing" -> true, "needsLogging" -> true))

      config.auditing shouldBe true
      config.logging  shouldBe true
    }

    "return all `false` when config is set to that" in {
      val config = ControllerConfig.fromConfig(Configuration("needsAuditing" -> false, "needsLogging" -> false))

      config.auditing shouldBe false
      config.logging  shouldBe false
    }
  }

  "ControllerConfigs.fromConfig" should {
    val controllerConfigs = ControllerConfigs.fromConfig(
      Configuration(
        "controllers.foo.needsAuditing"       -> false,
        "controllers.foo.needsLogging"        -> false,
        "controllers.a.b.c.bar.needsAuditing" -> false,
        "controllers.a.b.c.bar.needsLogging"  -> false
      ))

    "return loaded configuration" in {
      val `a.b.c.bar config` = controllerConfigs.get("a.b.c.bar")

      `a.b.c.bar config`.auditing shouldBe false
      `a.b.c.bar config`.logging  shouldBe false

      val fooConfig = controllerConfigs.get("foo")

      fooConfig.auditing shouldBe false
      fooConfig.logging  shouldBe false
    }

    "return default configuration for missing controllers" in {
      val config = controllerConfigs.get("bar")

      config.auditing shouldBe true
      config.logging  shouldBe true
    }

    "not fail if there are primitive values with controllers. prefix" in {
      val controllerConfigsWithPrimitiveValues = ControllerConfigs.fromConfig(
        Configuration(
          "controllers.foo.needsAuditing" -> false,
          "controllers.foo.needsLogging"  -> false
        ))

      val config = controllerConfigsWithPrimitiveValues.get("bar")

      config.auditing shouldBe true
      config.logging  shouldBe true
    }
  }
}
