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
import play.api.mvc.Headers

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

  def from(headers: Headers, redactions: Redactions): AuditableRequestHeaders = {
    val updatedCookies =
      headers
        .getAll("Cookie")
        .map(_.split("; ").filterNot { cookie =>
              val name = cookie.takeWhile(_ != '=')
              redactions.cookies.contains(name)
            }
          .mkString("; ")
        )
        .filterNot(_.isEmpty)

    val updatedHeaders =
      headers
        .remove("Cookie")
        .add(updatedCookies.map("Cookie" -> _): _*)
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
