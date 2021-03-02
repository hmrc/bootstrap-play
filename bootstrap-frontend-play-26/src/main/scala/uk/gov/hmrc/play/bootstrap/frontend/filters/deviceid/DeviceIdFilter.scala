/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.play.bootstrap.frontend.filters.deviceid

import play.api.mvc._
import play.api.mvc.request.{Cell, RequestAttrKey}
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{DataEvent, EventTypes}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

trait DeviceIdFilter extends Filter with DeviceIdCookie {

  protected implicit def ec: ExecutionContext

  protected def auditConnector: AuditConnector
  protected def appName: String

  override def apply(next: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
    val requestCookies = rh.attrs(RequestAttrKey.Cookies).value

    def allCookiesApartFromDeviceId = requestCookies.filterNot(_.name == DeviceId.MdtpDeviceId)

    val cookieResult = requestCookies
      .find(deviceIdCookie)
      .map { deviceCookeValueId =>
        DeviceId.from(deviceCookeValueId.value, secret, previousSecrets) match {

          case Some(deviceId) =>
            // Valid new format cookie.
            // Ensure the cookie has appropriate 'secure' flag by setting it again, this will also extend the life of the cookie
            val secureDeviceIdCookie = buildNewDeviceIdCookie().copy(value = deviceId.value)
            CookieResult(allCookiesApartFromDeviceId.toSeq :+ secureDeviceIdCookie, secureDeviceIdCookie)

          case None =>
            // Invalid deviceId cookie. Replace invalid cookie from request with new deviceId cookie and return in response.
            val deviceIdCookie = buildNewDeviceIdCookie()
            sendBadDeviceDataEvent(rh, deviceCookeValueId.value, deviceIdCookie.value)
            CookieResult(allCookiesApartFromDeviceId.toSeq :+ deviceIdCookie, deviceIdCookie)
        }
      }
      .getOrElse {
        // No deviceId cookie found or empty cookie value. Create new deviceId cookie, add to request and response.
        val newDeviceIdCookie = buildNewDeviceIdCookie()
        CookieResult(allCookiesApartFromDeviceId.toSeq :+ newDeviceIdCookie, newDeviceIdCookie)
      }

    val rhUpdatedWithDeviceIdCookie =
      rh.addAttr(
        key   = RequestAttrKey.Cookies,
        value = Cell(Cookies(cookieResult.cookies))
      )

    next(rhUpdatedWithDeviceIdCookie).map { result =>
      result.withCookies(cookieResult.newDeviceIdCookie)
    }
  }

  private def deviceIdCookie(cookie: Cookie): Boolean = cookie.name == DeviceId.MdtpDeviceId && !cookie.value.isEmpty

  private case class CookieResult(cookies: Seq[Cookie], newDeviceIdCookie: Cookie)

  private def sendBadDeviceDataEvent(rh: RequestHeader, badDeviceId: String, goodDeviceId: String): Unit = {
    val hc = HeaderCarrierConverter.fromRequestAndSession(rh, rh.session)
               // for backward compatibility, clear (the bad) deviceID, since we are sending explicitly in the event detail
               .copy(deviceID = None)
    auditConnector.sendEvent(
      DataEvent(
        appName,
        EventTypes.Failed,
        tags   = hc.toAuditTags("deviceIdFilter", rh.path),
        detail = getTamperDetails(badDeviceId, goodDeviceId)
      )
    )
  }

  private def getTamperDetails(tamperDeviceId: String, newDeviceId: String) =
    Map("tamperedDeviceId" -> tamperDeviceId, "deviceID" -> newDeviceId)
}
