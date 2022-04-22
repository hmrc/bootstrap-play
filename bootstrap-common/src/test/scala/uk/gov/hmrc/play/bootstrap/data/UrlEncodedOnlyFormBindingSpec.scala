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

package uk.gov.hmrc.play.bootstrap.data

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.mvc.Http.HeaderNames.CONTENT_TYPE
import uk.gov.hmrc.play.bootstrap.data.MultipartGenerator.aMultipartFileWithParams

class UrlEncodedOnlyFormBindingSpec extends AnyWordSpecLike with Matchers {

  val fixture = new UrlEncodedOnlyFormBinding()

  "url encoded only form binding" should {
    "work with application/x-www-form-urlencoded" in {
      val request = FakeRequest("POST", "/test")
        .withHeaders(CONTENT_TYPE -> "application/x-www-form-urlencoded")
        .withFormUrlEncodedBody("test" -> "test")

      val result = fixture.apply(request)
      result should contain("test" -> Seq("test"))
    }

    "not work with application/json" in {
      val request = FakeRequest("POST", "/test")
        .withHeaders(CONTENT_TYPE -> "application/json")
        .withJsonBody(Json.parse("""{"test": "test"}"""))

      val result = fixture.apply(request)
      result shouldBe empty
    }

    "not work with multipart/form-data" in {
      val request = FakeRequest("POST", "/test")
        .withHeaders(CONTENT_TYPE -> "multipart/form-data")
        .withMultipartFormDataBody(aMultipartFileWithParams("test" -> Seq("test")))

      val result = fixture.apply(request)
      result shouldBe empty
    }
  }

}
