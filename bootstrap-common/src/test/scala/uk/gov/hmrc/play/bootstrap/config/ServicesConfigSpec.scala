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

package uk.gov.hmrc.play.bootstrap.config

import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration

import scala.concurrent.duration._

class ServicesConfigSpec extends AnyWordSpecLike with Matchers with MockitoSugar {

  private val servicesConfig =
    new ServicesConfig(Configuration(
      "microservice.services.testString" -> "hello world",
      "microservice.services.testInt"    -> "1",
      "microservice.services.testBool"   -> "true",
      "microservice.services.testDur"    -> "60seconds",
      "anotherInt"                       -> "1",
      "anotherString"                    -> "hello other test",
      "anotherBool"                      -> "false",
      "anotherDur"                       -> "60seconds"
    ))

  import servicesConfig._

  "getConfString" should {
    "return a string from config under microservice.services" in {
      getConfString("testString", "") shouldBe "hello world"
    }

    "return a default string if the config can't be found" in {
      getConfString("notInConf", "hello default") shouldBe "hello default"
    }
  }

  "getConfInt" should {
    "return an int from config under microservice.services" in {
      getConfInt("testInt", 0) shouldBe 1
    }

    "return a default int if the config can't be found" in {
      getConfInt("notInConf", 1) shouldBe 1
    }
  }

  "getConfBool" should {
    "return a boolean from config under microservice.services" in {
      getConfBool("testBool", defBool = false) shouldBe true
    }

    "return a default boolean if the config can't be found" in {
      getConfBool("notInConf", defBool = true) shouldBe true
    }
  }

  "getConfDuration" should {
    "return a Duration from config under microservice.services" in {
      getConfDuration("testDur", 60.minutes) shouldBe 60.seconds
    }

    "return a default Duration if the config can't be found" in {
      getConfDuration("notInConf", 60.seconds) shouldBe 60.seconds
    }
  }

  "getInt" should {
    "return an int from config" in {
      getInt("anotherInt") shouldBe 1
    }

    "throw an exception if the config can't be found" in {
      intercept[RuntimeException](getInt("notInConf")).getMessage shouldBe "Could not find config key 'notInConf'"
    }
  }

  "getString" should {
    "return a string from config" in {
      getString("anotherString") shouldBe "hello other test"
    }

    "throw an exception if the config can't be found" in {
      intercept[RuntimeException](getString("notInConf")).getMessage shouldBe "Could not find config key 'notInConf'"
    }
  }

  "getBool" should {
    "return a boolean from config" in {
      getBoolean("anotherBool") shouldBe false
    }

    "throw an exception if the config can't be found" in {
      intercept[RuntimeException](getBoolean("notInConf")).getMessage shouldBe "Could not find config key 'notInConf'"
    }
  }

  "getDuration" should {
    "return a Duration from config" in {
      getDuration("anotherDur") shouldBe 60.seconds
    }

    "throw an exception if the config can't be found" in {
      intercept[RuntimeException](getDuration("notInConf")).getMessage shouldBe "Could not find config key 'notInConf'"
    }
  }
}
