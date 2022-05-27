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
import play.api.mvc.Headers
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
          "Cookie" -> "c1=v",
          "Cookie" -> "c2=v; c3=v; c4=v",
          "Cookie" -> "c5=v; c6=v"
        )

      AuditableRequestHeaders.from(headers, Redactions.empty).headers shouldBe headers
    }

    "Redact all values that correspond to a header, when configured" in {
      val headers =
        Headers(
          "Some-Header-1" -> "Some-Value",
          "Some-Header-1" -> "Some-Value",
          "Some-Header-2" -> "Some-Value",
          "Some-Header-3" -> "Some-Value",
        )

      val redactions =
        Redactions(
          headers = Set("Some-Header-1", "Some-Header-2"),
          cookies = Set.empty
        )

      val expectedHeaders =
        Headers(
          "Some-Header-3" -> "Some-Value"
        )

      AuditableRequestHeaders.from(headers, redactions).headers shouldBe expectedHeaders
    }

    "Redact individual cookies by name, when configured" in {
      val headers =
        Headers(
          "Some-Header-1" -> "Some-Value",
          "Some-Header-1" -> "Some-Value",
          "Cookie" -> "c1=v",
          "Cookie" -> "c2=v; c3=v; c4=v",
          "Cookie" -> "c5=v; c6=v; c7=v",
          "Cookie" -> "c8=v; c9=v; c10=v",
          "Cookie" -> "c11=v; c12=v; c13=v",
        )

      val redactions =
        Redactions(
          headers = Set.empty,
          cookies = Set("c1", "c2", "c7", "c8", "c9", "c10")
        )

      val expectedHeaders =
        Headers(
          "Some-Header-1" -> "Some-Value",
          "Some-Header-1" -> "Some-Value",
          "Cookie" -> "c3=v; c4=v",
          "Cookie" -> "c5=v; c6=v",
          "Cookie" -> "c11=v; c12=v; c13=v",
        )

      AuditableRequestHeaders.from(headers, redactions).headers shouldBe expectedHeaders
    }

    "Redact the entire `Cookie` header if all cookies are configured to be redacted" in {
      val headers =
        Headers(
          "Some-Header-1" -> "Some-Value",
          "Some-Header-1" -> "Some-Value",
          "Cookie" -> "c1=v",
          "Cookie" -> "c2=v; c3=v; c4=v"
        )

      val redactions =
        Redactions(
          headers = Set.empty,
          cookies = Set("c1", "c2", "c3", "c4")
        )

      val expectedHeaders =
        Headers(
          "Some-Header-1" -> "Some-Value",
          "Some-Header-1" -> "Some-Value",
        )

      AuditableRequestHeaders.from(headers, redactions).headers shouldBe expectedHeaders
    }
  }
}
