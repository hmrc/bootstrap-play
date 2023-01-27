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
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{Reads, __}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier

class HttpAuditEventSpec extends AnyWordSpecLike with Matchers with GuiceOneAppPerSuite {

  "The code to generate audit events" should {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    object HttpAuditEventForTest extends HttpAuditEvent {
      override def appName: String = "my-test-app"
    }

    "create valid audit events with optional headers" in {
      val request =
        FakeRequest().withHeaders("Foo" -> "Bar", "Ehh" -> "Meh", "Surrogate" -> "Cool", "Surrogate" -> "Cool")

      val extendedEvent = HttpAuditEventForTest.extendedEvent("foo", "bar", request)
      extendedEvent.detail.as(Reads.at[String](__ \ "surrogate")) shouldBe "Cool,Cool"

      val event = HttpAuditEventForTest.dataEvent("foo", "bar", request)
      event.detail.get("surrogate") shouldBe Some("Cool,Cool")
    }

    "create valid audit events with no optional headers" in {
      val request = FakeRequest().withHeaders("Foo" -> "Bar", "Ehh" -> "Meh")

      val extendedEvent = HttpAuditEventForTest.extendedEvent("foo", "bar", request)
      extendedEvent.detail.as(Reads.nullable[String](__ \ "surrogate")) shouldBe None

      val event = HttpAuditEventForTest.dataEvent("foo", "bar", request)
      event.detail.get("surrogate") shouldBe None
    }
  }
}
