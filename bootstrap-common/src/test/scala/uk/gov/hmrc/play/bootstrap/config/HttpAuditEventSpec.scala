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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.JsString
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.audit.syntax._

class HttpAuditEventSpec extends AnyWordSpecLike with Matchers with GuiceOneAppPerSuite {

  "The code to generate an audit event" should {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    object HttpAuditEventForTest extends HttpAuditEvent {
      override def appName: String = "my-test-app"
    }

    "create a valid audit event with optional headers" in {
      val request =
        FakeRequest().withHeaders("Foo" -> "Bar", "Ehh" -> "Meh", "Surrogate" -> "Cool", "Surrogate" -> "Cool")
      val event = HttpAuditEventForTest.extendedEvent("foo", "bar", request)
      event.detail.asJsObjectMap.get("surrogate") shouldBe Some(JsString("Cool,Cool"))
    }

    "create a valid audit event with no optional headers" in {
      val request     = FakeRequest().withHeaders("Foo" -> "Bar", "Ehh" -> "Meh")
      val event = HttpAuditEventForTest.extendedEvent("foo", "bar", request)
      event.detail.asJsObjectMap.get("surrogate") shouldBe None
    }
  }
}
