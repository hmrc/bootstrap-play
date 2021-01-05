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

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import org.scalatest.matchers.should.Matchers
import org.mockito.Mockito.{times, _}
import org.mockito.{ArgumentCaptor, Mockito}
import org.mockito.ArgumentMatchers.any
import org.scalatest.{BeforeAndAfterAll, OptionValues}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc._
import play.api.mvc.request.RequestAttrKey
import play.api.test.FakeRequest
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{DataEvent, EventTypes}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.reflectiveCalls

class DeviceIdFilterSpec
    extends AnyWordSpecLike
    with Matchers
    with ScalaFutures
    with MockitoSugar
    with BeforeAndAfterAll
    with OptionValues {

  lazy val timestamp = System.currentTimeMillis()
  implicit val system = ActorSystem("DeviceIdFilterSpec")

  override def afterAll(): Unit = {
    system.terminate()
  }

  private trait Setup extends Results {
    val normalCookie = Cookie("AnotherCookie1", "normalValue1")

    val resultFromAction: Result = Ok

    lazy val action = {
      val mockAction       = mock[(RequestHeader) => Future[Result]]
      val outgoingResponse = Future.successful(resultFromAction)
      when(mockAction.apply(any())).thenReturn(outgoingResponse)
      mockAction
    }

    lazy val filter = new DeviceIdFilter {
      implicit val mat: Materializer = ActorMaterializer()

      lazy val mdtpCookie = super.buildNewDeviceIdCookie()

      override def getTimeStamp = timestamp

      override def buildNewDeviceIdCookie() = mdtpCookie

      override val secret          = "SOME_SECRET"
      override val previousSecrets = Seq("previous_key_1", "previous_key_2")

      override val appName = "SomeAppName"

      lazy val auditConnector = mock[AuditConnector]

      override protected implicit def ec: ExecutionContext = ExecutionContext.global
    }

    lazy val newFormatGoodCookieDeviceId = filter.mdtpCookie

    def requestPassedToAction(time: Option[Int] = None): RequestHeader = {
      val updatedRequest = ArgumentCaptor.forClass(classOf[RequestHeader])
      verify(action, time.fold(times(1))(count => Mockito.atMost(count))).apply(updatedRequest.capture())
      updatedRequest.getValue
    }

    def mdtpdiSetCookie(result: Result): Cookie =
      result.newCookies.find(_.name == DeviceId.MdtpDeviceId).value

    def expectAuditIdEvent(badCookie: String, validCookie: String) = {
      val captor = ArgumentCaptor.forClass(classOf[DataEvent])
      verify(filter.auditConnector).sendEvent(captor.capture())(any(), any())
      val event = captor.getValue

      event.auditType   shouldBe EventTypes.Failed
      event.auditSource shouldBe "SomeAppName"

      event.detail should contain("tamperedDeviceId" -> badCookie)
      event.detail should contain("deviceID"         -> validCookie)
    }

    def invokeFilter(cookies: Seq[Cookie], expectedResultCookie: Cookie, times: Option[Int] = None) = {
      val incomingRequest = FakeRequest().withCookies(cookies: _*)
      val result          = filter(action)(incomingRequest).futureValue

      val expectedCookie =
        requestPassedToAction(times)
          .attrs(RequestAttrKey.Cookies)
          .value
          .find(_.name == DeviceId.MdtpDeviceId)
          .value

      expectedCookie.value shouldBe expectedResultCookie.value

      result
    }
  }

  "The filter supporting multiple previous hash secrets" should {

    "successfully validate the hash of deviceId's built from more than one previous key" in new Setup {

      for (prevSecret <- filter.previousSecrets) {
        val uuid                    = filter.generateUUID
        val timestamp               = filter.getTimeStamp
        val deviceIdMadeFromPrevKey = DeviceId(uuid, timestamp, DeviceId.generateHash(uuid, timestamp, prevSecret))
        val cookie                  = filter.makeCookie(deviceIdMadeFromPrevKey)

        val result = invokeFilter(Seq(cookie), cookie, Some(2))

        val responseCookie = mdtpdiSetCookie(result)
        responseCookie.value  shouldBe deviceIdMadeFromPrevKey.value
        responseCookie.secure shouldBe true
      }
    }
  }

  "During request pre-processing, the filter" should {

    "create a new deviceId if the deviceId cookie received contains an empty value " in new Setup {
      val result = invokeFilter(Seq(newFormatGoodCookieDeviceId.copy(value = "")), newFormatGoodCookieDeviceId)

      val responseCookie = mdtpdiSetCookie(result)
      responseCookie.value  shouldBe newFormatGoodCookieDeviceId.value
      responseCookie.secure shouldBe true
    }

    "create new deviceId cookie when no cookies exists" in new Setup {
      val result = invokeFilter(Seq.empty, newFormatGoodCookieDeviceId)

      val responseCookie = mdtpdiSetCookie(result)
      responseCookie.value  shouldBe newFormatGoodCookieDeviceId.value
      responseCookie.secure shouldBe true
    }

    "not change the request or the response when a valid new format mtdpdi cookie exists" in new Setup {
      val result = invokeFilter(Seq(newFormatGoodCookieDeviceId, normalCookie), newFormatGoodCookieDeviceId)

      val expectedCookie1 = requestPassedToAction().cookies.get("AnotherCookie1").get
      val expectedCookie2 = requestPassedToAction().cookies.get(DeviceId.MdtpDeviceId).get

      expectedCookie1.value shouldBe "normalValue1"
      expectedCookie2.value shouldBe newFormatGoodCookieDeviceId.value

      val responseCookie = mdtpdiSetCookie(result)
      responseCookie.value  shouldBe newFormatGoodCookieDeviceId.value
      responseCookie.secure shouldBe true
    }

    "make an insecure mtdpdi cookie secure, keeping the same value" in new Setup {
      val result =
        invokeFilter(Seq(newFormatGoodCookieDeviceId, normalCookie), newFormatGoodCookieDeviceId.copy(secure = false))

      val expectedCookie1 = requestPassedToAction().cookies.get("AnotherCookie1").get
      val expectedCookie2 = requestPassedToAction().cookies.get(DeviceId.MdtpDeviceId).get

      expectedCookie1.value shouldBe "normalValue1"
      expectedCookie2.value shouldBe newFormatGoodCookieDeviceId.value

      val responseCookie = mdtpdiSetCookie(result)
      responseCookie.value  shouldBe newFormatGoodCookieDeviceId.value
      responseCookie.secure shouldBe true
    }

    "identify new format deviceId cookie has invalid hash and create new deviceId cookie" in new Setup {

      val newFormatBadCookieDeviceId = {
        val deviceId = filter.generateDeviceId().copy(hash = "wrongvalue")
        Cookie(DeviceId.MdtpDeviceId, deviceId.value, Some(DeviceId.TenYears))
      }

      val result = invokeFilter(Seq(newFormatBadCookieDeviceId), newFormatGoodCookieDeviceId)

      val responseCookie = mdtpdiSetCookie(result)
      responseCookie.value  shouldBe newFormatGoodCookieDeviceId.value
      responseCookie.secure shouldBe true

      expectAuditIdEvent(newFormatBadCookieDeviceId.value, newFormatGoodCookieDeviceId.value)
    }

    "identify new format deviceId cookie has invalid timestamp and create new deviceId cookie" in new Setup {

      val newFormatBadCookieDeviceId = {
        val deviceId = filter.generateDeviceId().copy(hash = "wrongvalue")
        Cookie(DeviceId.MdtpDeviceId, deviceId.value, Some(DeviceId.TenYears))
      }

      val result = invokeFilter(Seq(newFormatBadCookieDeviceId), newFormatGoodCookieDeviceId)

      val responseCookie = mdtpdiSetCookie(result)
      responseCookie.value  shouldBe newFormatGoodCookieDeviceId.value
      responseCookie.secure shouldBe true

      expectAuditIdEvent(newFormatBadCookieDeviceId.value, newFormatGoodCookieDeviceId.value)
    }

    "identify new format deviceId cookie has invalid prefix and create new deviceId cookie" in new Setup {

      val newFormatBadCookieDeviceId = {
        val deviceId = filter.generateDeviceId()
        Cookie(
          DeviceId.MdtpDeviceId,
          deviceId.value.replace(DeviceId.MdtpDeviceId, "BAD_PREFIX"),
          Some(DeviceId.TenYears))
      }

      val result = invokeFilter(Seq(newFormatBadCookieDeviceId), newFormatGoodCookieDeviceId)

      val responseCookie = mdtpdiSetCookie(result)
      responseCookie.value  shouldBe newFormatGoodCookieDeviceId.value
      responseCookie.secure shouldBe true
    }
  }
}
