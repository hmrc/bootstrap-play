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
import com.typesafe.config.ConfigFactory

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

    "support nested when quoted" in {
      val logger1       = "asd"
      val logger2       = "asd.xyz"

      val configuration =
        Configuration(
          s"""logger."$logger1"""" -> Level.WARN.toString,
          s"""logger."$logger2"""" -> Level.INFO.toString
        )
      new LoggerModule()
        .bindings(
          environment   = Environment.simple(),
          configuration = configuration
        )

      LoggerFactory.getLogger(logger1).asInstanceOf[Logger].getLevel shouldBe Level.WARN
      LoggerFactory.getLogger(logger2).asInstanceOf[Logger].getLevel shouldBe Level.INFO
    }

    "support nested when provided as system.properties" in {
      val logger1       = "asd"
      val logger2       = "asd.xyz"

      val configuration =
        Configuration(
          ConfigFactory.parseString(
            s"""
               |logger.$logger1: ${Level.WARN}
               |logger.$logger2: ${Level.INFO}
            """.stripMargin
          )
        )
      // when merging an object with a value (here "asd") - the second one wins
      configuration.entrySet.collect { case (k, v) if k.startsWith("logger.") => k -> v.unwrapped} shouldBe Set(s"logger.$logger2" -> Level.INFO.toString)

      // but if the values are available as System.properites - they should be preserved (without needing to quote)
      val init = System.getProperties
      System.setProperty(s"logger.$logger1", Level.WARN.toString)
      System.setProperty(s"logger.$logger2", Level.INFO.toString)

      new LoggerModule()
        .bindings(
          environment   = Environment.simple(),
          configuration = configuration
        )

      LoggerFactory.getLogger(logger1).asInstanceOf[Logger].getLevel shouldBe Level.WARN
      LoggerFactory.getLogger(logger2).asInstanceOf[Logger].getLevel shouldBe Level.INFO

      System.setProperties(init)
    }
  }
}
