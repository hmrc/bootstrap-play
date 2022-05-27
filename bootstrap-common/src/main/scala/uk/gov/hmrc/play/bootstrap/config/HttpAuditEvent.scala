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

import com.google.inject.ImplementedBy
import play.api.libs.json.{JsObject, JsString, Json}

import javax.inject.{Inject, Named}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.model.{ExtendedDataEvent, TruncationLog}

class DefaultHttpAuditEvent @Inject()(
  @Named("appName") val appName: String
) extends HttpAuditEvent

@ImplementedBy(classOf[DefaultHttpAuditEvent])
trait HttpAuditEvent {

  def appName: String

  protected object auditDetailKeys {
    val Input           = "input"
    val Method          = "method"
    val UserAgentString = "userAgentString"
    val Referrer        = "referrer"
  }

  protected object headers {
    val UserAgent = "User-Agent"
    val Referer   = "Referer"
  }

  def extendedEvent(
    eventType      : String,
    transactionName: String,
    request        : RequestHeader,
    detail         : JsObject              = JsObject.empty,
    truncationLog  : Option[TruncationLog] = None
  )(implicit
    hc: HeaderCarrier
  ): ExtendedDataEvent = {
    import auditDetailKeys._
    import headers._
    import uk.gov.hmrc.play.audit.http.HeaderFieldsExtractor._

    val requiredFields =
      Json.obj(
        "ipAddress"            -> hc.forwarded.map(_.value).getOrElse[String]("-"),
        hc.names.authorisation -> hc.authorization.map(_.value).getOrElse[String]("-"),
        hc.names.deviceID      -> hc.deviceID.getOrElse[String]("-"),
        Input                  -> s"Request to ${request.path}",
        Method                 -> request.method.toUpperCase,
        UserAgentString        -> request.headers.get(UserAgent).getOrElse[String]("-"),
        Referrer               -> request.headers.get(Referer).getOrElse[String]("-")
      )

    val tags = hc.toAuditTags(transactionName, request.path)

    ExtendedDataEvent(
      appName,
      eventType,
      detail        = detail ++ requiredFields ++ JsObject(optionalAuditFieldsSeq(request.headers.toMap).mapValues(JsString)),
      tags          = tags,
      truncationLog = truncationLog
    )
  }
}
