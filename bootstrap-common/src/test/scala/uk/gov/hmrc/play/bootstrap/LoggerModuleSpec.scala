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

package uk.gov.hmrc.play.bootstrap

import ch.qos.logback.classic.{Level, Logger}
import play.api.{Configuration, Environment}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.slf4j.LoggerFactory

class LoggerModuleSpec
  extends AnyWordSpec
     with Matchers  {

  "LoggerModule" should {
    "turn on logging as per configuration" in {
      val logger       = "xyz"
      val desiredLevel = Level.WARN

      LoggerFactory.getLogger(logger).asInstanceOf[Logger].getLevel should not be desiredLevel

      val configuration =
        Configuration(
          s"logger.$logger" -> desiredLevel.toString
        )
      new LoggerModule()
        .bindings(
          environment   = Environment.simple(),
          configuration = configuration
        )

      LoggerFactory.getLogger(logger).asInstanceOf[Logger].getLevel shouldBe desiredLevel
    }

    "ignore non-logging configuration" in {
      val logger1       = "asd"
      val logger2       = "xyz"

      val loggerLevel1 = LoggerFactory.getLogger(logger1).asInstanceOf[Logger].getLevel
      val loggerLevel2 = LoggerFactory.getLogger(logger2).asInstanceOf[Logger].getLevel

      val configuration =
        Configuration(
          s"logger.$logger1" -> "invalid String",
          s"logger.$logger2" -> Map("a" -> "b")
        )
      new LoggerModule()
        .bindings(
          environment   = Environment.simple(),
          configuration = configuration
        )

      LoggerFactory.getLogger(logger1).asInstanceOf[Logger].getLevel shouldBe loggerLevel1
      LoggerFactory.getLogger(logger2).asInstanceOf[Logger].getLevel shouldBe loggerLevel2
    }
  }
}
