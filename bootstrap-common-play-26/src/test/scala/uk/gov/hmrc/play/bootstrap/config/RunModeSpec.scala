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

package uk.gov.hmrc.play.bootstrap.config

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.{Configuration, Mode}

class RunModeSpec extends AnyWordSpecLike with Matchers {

  "env" should {
    "return 'Test' if Play mode is set to Test" in {
      new RunMode(Configuration(), Mode.Test).env shouldBe "Test"
    }

    "return value from 'run.mode' property if Play mode is not Test" in {
      new RunMode(Configuration("run.mode" -> "Something"), Mode.Prod).env shouldBe "Something"
    }

    "return Dev as a default if Play mode is not Test" in {
      new RunMode(Configuration(), Mode.Prod).env shouldBe "Dev"
      new RunMode(Configuration(), Mode.Dev).env  shouldBe "Dev"
    }
  }

  "envPath" should {
    "return the `other` env path" in {
      val runMode = new RunMode(Configuration(), Mode.Dev)
      import runMode.envPath

      envPath("/somePath")(other  = "http://localhost")  shouldBe "http://localhost/somePath"
      envPath("/somePath")(other  = "http://localhost/") shouldBe "http://localhost/somePath"
      envPath("//somePath")(other = "http://localhost")  shouldBe "http://localhost/somePath"
      envPath("somePath")(other   = "http://localhost")  shouldBe "http://localhost/somePath"
      envPath("somePath")(other   = "http://localhost/") shouldBe "http://localhost/somePath"
      envPath("somePath/")(other  = "http://localhost/") shouldBe "http://localhost/somePath"
      envPath()(other             = "http://localhost")  shouldBe "http://localhost"
    }

    "return the `prod` env path" in {
      val runMode = new RunMode(Configuration("run.mode" -> "Prod"), Mode.Prod)
      import runMode.envPath

      envPath("/somePath")(prod  = "prod")    shouldBe "/prod/somePath"
      envPath("/somePath")(prod  = "/prod")   shouldBe "/prod/somePath"
      envPath("/somePath")(prod  = "//prod")  shouldBe "/prod/somePath"
      envPath("/somePath")(prod  = "prod/")   shouldBe "/prod/somePath"
      envPath("/somePath")(prod  = "/prod/")  shouldBe "/prod/somePath"
      envPath("/somePath")(prod  = "//prod/") shouldBe "/prod/somePath"
      envPath("//somePath")(prod = "/prod/")  shouldBe "/prod/somePath"
      envPath("somePath/")(prod  = "prod")    shouldBe "/prod/somePath"
      envPath()(prod             = "prod")    shouldBe "/prod"
    }
  }
}
