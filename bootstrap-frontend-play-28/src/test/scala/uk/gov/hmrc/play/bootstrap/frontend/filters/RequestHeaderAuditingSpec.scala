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
import play.api.Configuration
import play.api.mvc.{Cookie, Cookies, DefaultCookieHeaderEncoding, Headers}
import uk.gov.hmrc.play.bootstrap.frontend.filters.RequestHeaderAuditing.redactedValue

class RequestHeaderAuditingSpec
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

      requestHeaderAuditing(
        redactedHeaders = Set.empty,
        redactedCookies = Set.empty
      ).auditableHeaders(headers, cookies).headers shouldBe expectedHeaders
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

      val expectedHeaders =
        Headers(
          "Some-Header-1" -> redactedValue,
          "Some-Header-1" -> redactedValue,
          "Some-Header-2" -> redactedValue,
          "Some-Header-3" -> "Some-Value",
        )

      requestHeaderAuditing(
        redactedHeaders = Set("Some-Header-1", "Some-Header-2"),
        redactedCookies = Set.empty
      ).auditableHeaders(headers, cookies).headers shouldBe expectedHeaders
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

      val expectedHeaders =
        Headers(
          "Some-Header-1" -> "Some-Value",
          "Some-Header-1" -> "Some-Value",
          "Cookie" -> s"c1=$redactedValue; c2=v; c3=$redactedValue; c4=v; c5=$redactedValue",
        )

      requestHeaderAuditing(
        redactedHeaders = Set.empty,
        redactedCookies = Set("c1", "c3", "c5")
      ).auditableHeaders(headers, cookies).headers shouldBe expectedHeaders
    }
  }

  private def requestHeaderAuditing(redactedHeaders: Set[String], redactedCookies: Set[String]) = {
    val cookieHeaderEncoding =
      new DefaultCookieHeaderEncoding()

    val config =
      Configuration(
        "bootstrap.auditfilter.frontend.auditAllHeaders" -> true,
        "bootstrap.auditfilter.frontend.redactedHeaders" -> redactedHeaders.toSeq,
        "bootstrap.auditfilter.frontend.redactedCookies" -> redactedCookies.toSeq,
      )

    new RequestHeaderAuditing(new RequestHeaderAuditing.Config(config), cookieHeaderEncoding)
  }
}
