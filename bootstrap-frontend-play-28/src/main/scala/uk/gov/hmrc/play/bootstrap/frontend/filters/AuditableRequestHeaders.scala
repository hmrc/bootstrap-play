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

import play.api.Configuration
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{Cookies, DefaultCookieHeaderEncoding, Headers}

sealed trait AuditableRequestHeaders {
  def headers: Headers
}

object AuditableRequestHeaders {

  final case class Redactions(headers: Set[String], cookies: Set[String])

  object Redactions {
    def fromConfig(config: Configuration): Redactions =
      Redactions(
        headers = config.get[Seq[String]]("bootstrap.auditfilter.frontend.redactedHeaders").toSet,
        cookies = config.get[Seq[String]]("bootstrap.auditfilter.frontend.redactedCookies").toSet,
      )

    val empty: Redactions =
      Redactions(Set.empty, Set.empty)
  }

  def from(
    headers: Headers,
    cookies: Cookies,
    redactions: Redactions
  ): AuditableRequestHeaders = {
    val updatedCookies = {
      val updated =
        cookies.filterNot(cookie => redactions.cookies.contains(cookie.name))

      if (updated.nonEmpty)
        Some(new DefaultCookieHeaderEncoding().encodeCookieHeader(updated.toSeq))
      else None
    }

    val updatedHeaders =
      updatedCookies
        .fold(headers)(cookieHeader => headers.remove("Cookie").add("Cookie" -> cookieHeader))
        .remove(redactions.headers.toSeq: _*)

    AuditableRequestHeadersImpl(updatedHeaders)
  }

  private final case class AuditableRequestHeadersImpl(headers: Headers) extends AuditableRequestHeaders

  implicit val writes: Writes[AuditableRequestHeaders] = {
    val header =
      Writes[(String, Seq[String])] { case (name, values) =>
        Json.obj("name" -> name, "values" -> values)
      }

    Writes
      .seq(header)
      .contramap[AuditableRequestHeaders](_.headers.toMap.toSeq)
  }
}
