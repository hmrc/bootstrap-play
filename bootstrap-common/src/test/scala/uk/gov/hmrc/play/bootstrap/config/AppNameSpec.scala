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

package uk.gov.hmrc.play.bootstrap.config

import com.typesafe.config.ConfigException
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration

class AppNameSpec extends AnyWordSpec with Matchers {

  "AppName" should {
    "return the name of the application" in {
      AppName.fromConfiguration(Configuration("appName" -> "myApp")) shouldBe "myApp"
    }

    "return fallback name if application name not available" in {
      intercept[ConfigException.Missing](AppName.fromConfiguration(Configuration.empty))
    }
  }
}
