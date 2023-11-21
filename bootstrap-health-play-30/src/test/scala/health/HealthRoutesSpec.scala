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

package uk.gov.hmrc.play.health

import org.scalatest.TestData
import org.scalatestplus.play.WsScalaTestClient
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.test._

import scala.language.implicitConversions

class HealthRoutesSpec
  extends AnyWordSpec
  with Matchers
  with WsScalaTestClient
  with GuiceOneAppPerTest {

  implicit override def newAppForTest(testData: TestData): Application =
    new GuiceApplicationBuilder()
      .configure(testData.configMap ++ Map("play.http.router" -> "health.Routes"))
      .build()

  "The Ping endpoint" must {
    "respond with a 200 status code when the service is OK" in {
      status(route(app, FakeRequest("GET", "/ping/ping")).get) shouldBe 200
    }
  }
}
