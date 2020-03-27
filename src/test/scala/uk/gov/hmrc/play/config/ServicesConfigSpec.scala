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

package uk.gov.hmrc.play.config

import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.{RunMode, ServicesConfig}

import scala.concurrent.duration._

class ServicesConfigSpec extends WordSpecLike with Matchers with MockitoSugar {

  private val servicesConfig = {
    val configuration = Configuration(
      "microservice.services.testString"         -> "hello world",
      "Test.microservice.services.devTestString" -> "hello test",
      "microservice.services.testInt"            -> "1",
      "Test.microservice.services.devTestInt"    -> "1",
      "microservice.services.testBool"           -> "true",
      "Test.microservice.services.devTestBool"   -> "true",
      "microservice.services.testDur"            -> "60seconds",
      "Test.microservice.services.devTestDur"    -> "60seconds",
      "anotherInt"                               -> "1",
      "anotherString"                            -> "hello other test",
      "anotherBool"                              -> "false",
      "anotherDur"                               -> "60seconds"
    )
    val mockedRunMode = mock[RunMode]
    when(mockedRunMode.env).thenReturn("Test")
    new ServicesConfig(configuration, mockedRunMode)
  }

  import servicesConfig._

  "getConfString" should {
    "return a string from config under rootServices" in {
      getConfString("testString", "") shouldBe "hello world"
    }

    "return a string from config under Dev services" in {
      getConfString("devTestString", "") shouldBe "hello test"
    }

    "return a default string if the config can't be found" in {
      getConfString("notInConf", "hello default") shouldBe "hello default"
    }
  }

  "getConfInt" should {
    "return an int from config under rootServices" in {
      getConfInt("testInt", 0) shouldBe 1
    }

    "return an int from config under Dev services" in {
      getConfInt("devTestInt", 0) shouldBe 1
    }

    "return a default int if the config can't be found" in {
      getConfInt("notInConf", 1) shouldBe 1
    }
  }

  "getConfBool" should {
    "return a boolean from config under rootServices" in {
      getConfBool("testBool", defBool = false) shouldBe true
    }

    "return a boolean from config under Dev services" in {
      getConfBool("devTestBool", defBool = false) shouldBe true
    }

    "return a default boolean if the config can't be found" in {
      getConfBool("notInConf", defBool = true) shouldBe true
    }
  }

  "getConfDuration" should {
    "return a Duration from config under rootServices" in {
      getConfDuration("testDur", 60.minutes) shouldBe 60.seconds
    }

    "return a Duration from config under Dev services" in {
      getConfDuration("devTestDur", 60.minutes) shouldBe 60.seconds
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
