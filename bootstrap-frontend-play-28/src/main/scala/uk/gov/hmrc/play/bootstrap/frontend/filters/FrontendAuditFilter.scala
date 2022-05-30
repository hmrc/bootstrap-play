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

import akka.stream.Materializer

import javax.inject.Inject
import play.api.Configuration
import play.api.http.HeaderNames
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.mvc.{RequestHeader, ResponseHeader}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.hooks.Body
import uk.gov.hmrc.play.audit.EventKeys
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{ExtendedDataEvent, TruncationLog}
import uk.gov.hmrc.play.bootstrap.config.{ControllerConfigs, HttpAuditEvent}
import uk.gov.hmrc.play.bootstrap.filters.CommonAuditFilter
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import uk.gov.hmrc.play.bootstrap.frontend.filters.deviceid.DeviceFingerprint

import scala.concurrent.ExecutionContext

trait FrontendAuditFilter
  extends CommonAuditFilter
     with FrontendHeaderCarrierProvider {

  def maskedFormFields: Seq[String]

  def applicationPort: Option[Int]

  def requestHeaderAuditing: RequestHeaderAuditing

  private val textHtml = ".*(text/html).*".r

  override protected def buildRequestDetails(requestHeader: RequestHeader, requestBody: Body[String]): (JsObject, TruncationLog) = {
    val (requestBodyStr, isRequestTruncated) = requestBody match {
      case Body.Complete(b)  => (b, false)
      case Body.Truncated(b) => (b, true)
    }

    val requestDetails =
      Json.obj(
        EventKeys.RequestBody -> stripPasswords(requestHeader.contentType, requestBodyStr, maskedFormFields),
        "deviceFingerprint"   -> DeviceFingerprint.deviceFingerprintFrom(requestHeader),
        "host"                -> getHost(requestHeader),
        "port"                -> getPort,
        "queryString"         -> getQueryString(requestHeader.queryString)
      ) ++ requestHeaderAuditing.auditableHeadersAsJsObject(requestHeader.headers, requestHeader.cookies)

    val truncationLog =
      TruncationLog(truncatedFields = if (isRequestTruncated) List(EventKeys.RequestBody) else List.empty)

    (requestDetails, truncationLog)
  }

  override protected def buildResponseDetails(responseHeader: ResponseHeader, responseBody: Body[String], contentType: Option[String]): (JsObject, TruncationLog) = {
    val (responseBodyStr, isResponseTruncated) = responseBody match {
      case Body.Complete(b)  => (b, false)
      case Body.Truncated(b) => (b, true)
    }

    val responseDetails =
      Json.obj(
        EventKeys.StatusCode      -> responseHeader.status.toString,
        EventKeys.ResponseMessage -> filterResponseBody(contentType, responseBodyStr)
      ) ++
        JsObject(
          responseHeader
            .headers
            .get(HeaderNames.LOCATION)
            .map(loc => HeaderNames.LOCATION -> JsString(loc))
            .toSeq
        )

    val truncationLog =
      TruncationLog(truncatedFields = if (isResponseTruncated) List(EventKeys.ResponseMessage) else List.empty)

    (responseDetails, truncationLog)
  }

  private[filters] def getHost(request: RequestHeader): String =
    request.headers.get("Host").map(_.takeWhile(_ != ':')).getOrElse("-")

  private[filters] def getPort: String =
    applicationPort.map(_.toString).getOrElse("-")

  private[filters] def getQueryString(queryString: Map[String, Seq[String]]): String =
    cleanQueryStringForDatastream(
      queryString.map { case (k, vs) => k + ":" + vs.mkString(",") }.mkString("&")
    )

  private def cleanQueryStringForDatastream(queryString: String): String =
    queryString.trim match {
      case ""    => "-"
      case ":"   => "-" // play 2.5 FakeRequest now parses an empty query string into a two empty string params
      case other => other
    }

  private[filters] def stripPasswords(
    contentType     : Option[String],
    requestBody     : String,
    maskedFormFields: Seq[String]
  ): String =
    contentType match {
      case Some("application/x-www-form-urlencoded") =>
        maskedFormFields.foldLeft(requestBody)((maskedBody, field) =>
          maskedBody.replaceAll(field + """=.*?(?=&|$|\s)""", field + "=#########"))
      case _ => requestBody
    }

  private[filters] def filterResponseBody(contentType: Option[String], responseBody: String) =
    contentType
      .collect { case textHtml(a) => "<HTML>...</HTML>" }
      .getOrElse(responseBody)
}

class DefaultFrontendAuditFilter @Inject()(
  override val config: Configuration,
  controllerConfigs: ControllerConfigs,
  override val auditConnector: AuditConnector,
  httpAuditEvent: HttpAuditEvent,
  override val requestHeaderAuditing: RequestHeaderAuditing,
  override val mat: Materializer
)(implicit protected val ec: ExecutionContext
) extends FrontendAuditFilter {

  override val maskedFormFields: Seq[String] =
    config.get[Seq[String]]("bootstrap.auditfilter.maskedFormFields")

  override val applicationPort: Option[Int] = None

  override def controllerNeedsAuditing(controllerName: String): Boolean =
    controllerConfigs.controllerNeedsAuditing(controllerName)

  override def extendedDataEvent(
    eventType      : String,
    transactionName: String,
    request        : RequestHeader,
    detail         : JsObject,
    truncationLog  : Option[TruncationLog]
  )(implicit hc: HeaderCarrier): ExtendedDataEvent =
    httpAuditEvent.extendedEvent(
      eventType,
      transactionName,
      request,
      detail,
      truncationLog
    )
}
