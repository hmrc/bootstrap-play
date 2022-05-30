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

package uk.gov.hmrc.play.bootstrap.frontend.filters

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.mvc.{Cookie, Cookies, Headers}
import uk.gov.hmrc.play.bootstrap.frontend.filters.AuditableRequestHeaders.Redactions

class AuditableRequestHeadersSpec
  extends AnyWordSpec
     with Matchers {

  "Redaction" should {

    "Not occur when no redactions are configured" in {
      val headers =
        Headers(
          "Host" -> "localhost",
          "Some-Header-1" -> "Some-Value",
          "Some-Header-1" -> "Some-Value",
          "Some-Header-2" -> "Some-Value",
        )

      val cookies =
        Cookies(
          Seq(
            Cookie("c1", "v"),
            Cookie("c2", "v"),
            Cookie("c3", "v"),
            Cookie("c4", "v"),
          )
        )

      val expectedHeaders =
        Headers(
          "Host" -> "localhost",
          "Some-Header-1" -> "Some-Value",
          "Some-Header-1" -> "Some-Value",
          "Some-Header-2" -> "Some-Value",
          "Cookie" -> "c1=v; c2=v; c3=v; c4=v"
        )

      AuditableRequestHeaders.from(headers, cookies, Redactions.empty).headers shouldBe expectedHeaders
    }

    "Redact all values that correspond to a header, when configured" in {
      val headers =
        Headers(
          "Some-Header-1" -> "Some-Value",
          "Some-Header-1" -> "Some-Value",
          "Some-Header-2" -> "Some-Value",
          "Some-Header-3" -> "Some-Value",
        )

      val cookies = Cookies(Seq.empty)

      val redactions =
        Redactions(
          headers = Set("Some-Header-1", "Some-Header-2"),
          cookies = Set.empty
        )

      val expectedHeaders =
        Headers(
          "Some-Header-3" -> "Some-Value"
        )

      AuditableRequestHeaders.from(headers, cookies, redactions).headers shouldBe expectedHeaders
    }

    "Redact individual cookies by name, when configured" in {
      val headers =
        Headers(
          "Some-Header-1" -> "Some-Value",
          "Some-Header-1" -> "Some-Value",
        )

      val cookies =
        Cookies(
          Seq(
            Cookie("c1", "v"),
            Cookie("c2", "v"),
            Cookie("c3", "v"),
            Cookie("c4", "v"),
            Cookie("c5", "v"),
          )
        )

      val redactions =
        Redactions(
          headers = Set.empty,
          cookies = Set("c1", "c3", "c5")
        )

      val expectedHeaders =
        Headers(
          "Some-Header-1" -> "Some-Value",
          "Some-Header-1" -> "Some-Value",
          "Cookie" -> "c2=v; c4=v",
        )

      AuditableRequestHeaders.from(headers, cookies, redactions).headers shouldBe expectedHeaders
    }

    "Redact the entire `Cookie` header if all cookies are configured to be redacted" in {
      val headers =
        Headers(
          "Some-Header-1" -> "Some-Value",
          "Some-Header-1" -> "Some-Value",
        )

      val cookies =
        Cookies(
          Seq(
            Cookie("c1", "v"),
            Cookie("c2", "v"),
          )
        )

      val redactions =
        Redactions(
          headers = Set.empty,
          cookies = Set("c1", "c2")
        )

      val expectedHeaders =
        Headers(
          "Some-Header-1" -> "Some-Value",
          "Some-Header-1" -> "Some-Value",
        )

      AuditableRequestHeaders.from(headers, cookies, redactions).headers shouldBe expectedHeaders
    }
  }
}
