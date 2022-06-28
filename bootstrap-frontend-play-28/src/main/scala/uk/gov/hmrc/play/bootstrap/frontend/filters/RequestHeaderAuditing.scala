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
import play.api.libs.json.{JsObject, Json, OWrites}
import play.api.mvc.{CookieHeaderEncoding, Cookies, Headers}
import uk.gov.hmrc.play.audit.http.Data
import uk.gov.hmrc.play.bootstrap.frontend.filters.RequestHeaderAuditing.{AuditableRequestHeaders, AuditableRequestHeadersImpl, redactedValue}

import javax.inject.{Inject, Singleton}

@Singleton
class RequestHeaderAuditing @Inject()(
  config: RequestHeaderAuditing.Config,
  cookieHeaderEncoding: CookieHeaderEncoding
) {

  def auditableHeaders(headers: Headers, cookies: Cookies): AuditableRequestHeaders = {
    val headersToRedact =
      headers.keys & config.redactedHeaders

    val cookiesData =
      Data.traverse(cookies.toSeq)(cookie =>
        if (config.redactedCookies.contains(cookie.name))
          Data.redacted(cookie.copy(value = redactedValue))
        else
          Data.pure(cookie)
      ).map(cookies =>
        if (cookies.nonEmpty)
          Some(cookieHeaderEncoding.encodeCookieHeader(cookies))
        else
          None
      )

    val redactedHeaders =
      cookiesData
        .value
        .fold(headers)(cookies => headers.replace("Cookie" -> cookies))
        .replace(headersToRedact.toSeq.map(_ -> redactedValue): _*)

    val redactedHeaderNames =
      (headersToRedact ++ (if (cookiesData.isRedacted) Set("Cookie") else Set.empty))
        .map(headerName => s"requestHeaders.${headerName.toLowerCase}")

    AuditableRequestHeadersImpl(redactedHeaders, redactedHeaderNames)
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
    def redactedHeaderNames: Set[String]
  }

  object AuditableRequestHeaders {

    implicit val writes: OWrites[AuditableRequestHeaders] =
      OWrites[AuditableRequestHeaders]{ arh =>
        JsObject(arh.headers.toMap.map { case (name, values) =>
          name.toLowerCase -> Json.toJson(values)
        })
      }
  }

  private final case class AuditableRequestHeadersImpl(
    headers: Headers,
    redactedHeaderNames: Set[String]
  ) extends AuditableRequestHeaders

  val redactedValue: String = "########"
}
