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

package uk.gov.hmrc.play.bootstrap.controller

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.http.ContentTypes

class Utf8MimeTypesSpec extends AnyWordSpecLike with Matchers {

  "Controller minetypes" should {

    "have default application json" in {

      val controller                     = new ContentTypes {}
      val applicationJsonWithUtf8Charset = controller.JSON

      applicationJsonWithUtf8Charset should not be "application/json;charset=utf-8"
    }

    "have application json with utf8 character set" in {

      val controller                     = new ContentTypes with Utf8MimeTypes {}
      val applicationJsonWithUtf8Charset = controller.JSON

      applicationJsonWithUtf8Charset shouldBe "application/json;charset=utf-8"
    }

    "have text html with utf8 character set" in {

      val controller              = new ContentTypes with Utf8MimeTypes {}
      val textHtmlWithUtf8Charset = controller.HTML

      textHtmlWithUtf8Charset shouldBe "text/html;charset=utf-8"
    }
  }
}
