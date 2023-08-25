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

package uk.gov.hmrc.play.bootstrap.tools

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.slf4j.{Logger, LoggerFactory}
import play.api.LoggerLike

class LogCapturingSpec extends AnyWordSpec with Matchers with LogCapturing {

  class TestLogger extends LoggerLike {
    override val logger: Logger = LoggerFactory.getLogger("test-logger")
  }

  "capture log events for test assertions" in {
    val testLogger = new TestLogger
    val testClass = new TestClass(testLogger)

    withCaptureOfLoggingFrom(testLogger) { events =>
      val (expected1, expected2) = ("hello", "world")

      testClass.logMessage(expected1)
      testClass.logMessage(expected2)

      events.map(_.getMessage) mustBe Seq(expected1, expected2)
    }
  }

  private class TestClass(testLogger: TestLogger) {
    def logMessage(msg: String): Unit = testLogger.logger.info(msg)
  }

}
