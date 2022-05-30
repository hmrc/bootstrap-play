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
import play.api.libs.json.{JsObject, Json, Writes}
import play.api.mvc.{CookieHeaderEncoding, Cookies, Headers}
import uk.gov.hmrc.play.bootstrap.frontend.filters.RequestHeaderAuditing.{AuditableRequestHeaders, AuditableRequestHeadersImpl, redactedValue}

import javax.inject.{Inject, Singleton}

@Singleton
class RequestHeaderAuditing @Inject()(
  config: RequestHeaderAuditing.Config,
  cookieHeaderEncoding: CookieHeaderEncoding
) {

  def auditableHeaders(headers: Headers, cookies: Cookies): AuditableRequestHeaders = {
    val updatedCookies = {
      val updated =
        cookies
          .map(cookie =>
            if (config.redactedCookies.contains(cookie.name))
              cookie.copy(value = redactedValue)
            else
              cookie
          )

      if (updated.nonEmpty) Some(cookieHeaderEncoding.encodeCookieHeader(updated.toSeq)) else None
    }

    val updatedHeaders = {
      val replacements =
        (headers.keys & config.redactedHeaders)
          .toSeq
          .map(_ -> redactedValue)

      updatedCookies
        .fold(headers)(cookieHeader => headers.replace("Cookie" -> cookieHeader))
        .replace(replacements: _*)
    }

    AuditableRequestHeadersImpl(updatedHeaders)
  }

  def auditableHeadersAsJsObject(headers: Headers, cookies: Cookies): JsObject =
    if (config.enabled)
      Json.obj("requestHeaders" -> auditableHeaders(headers, cookies))
    else
      JsObject.empty
}

object RequestHeaderAuditing {

  class Config @Inject()(configuration: Configuration) {

    val enabled: Boolean =
      configuration.get[Boolean]("bootstrap.auditfilter.frontend.auditAllHeaders")

    val redactedHeaders: Set[String] =
      configuration.get[Seq[String]]("bootstrap.auditfilter.frontend.redactedHeaders").toSet

    val redactedCookies: Set[String] =
      configuration.get[Seq[String]]("bootstrap.auditfilter.frontend.redactedCookies").toSet
  }

  sealed trait AuditableRequestHeaders {
    def headers: Headers
  }

  object AuditableRequestHeaders {

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

  private final case class AuditableRequestHeadersImpl(headers: Headers) extends AuditableRequestHeaders

  val redactedValue: String = "########"
}
