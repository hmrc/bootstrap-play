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

package uk.gov.hmrc.play.bootstrap.frontend.filters.deviceid

import akka.actor.ActorSystem
import akka.stream.Materializer
import org.scalatest.matchers.should.Matchers
import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, OptionValues}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.wordspec.AnyWordSpecLike
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
      when(mockAction.apply(any)).thenReturn(outgoingResponse)
      mockAction
    }

    def makeFilter(secureCookie: Boolean) = new DeviceIdFilter {
      lazy val mdtpCookie = super.buildNewDeviceIdCookie()

      override def getTimeStamp = timestamp

      override def buildNewDeviceIdCookie() = mdtpCookie

      override val mat             = implicitly[Materializer]

      override val secret          = "SOME_SECRET"
      override val previousSecrets = Seq("previous_key_1", "previous_key_2")
      override val secure          = secureCookie

      override val appName = "SomeAppName"

      lazy val auditConnector = mock[AuditConnector](withSettings.lenient)

      override protected implicit def ec: ExecutionContext = ExecutionContext.global
    }
    lazy val filter = makeFilter(secureCookie = true)

    lazy val newFormatGoodCookieDeviceId = filter.mdtpCookie

    def requestPassedToAction(): RequestHeader = {
      val updatedRequest = ArgCaptor[RequestHeader]
      verify(action, times(1)).apply(updatedRequest.capture)
      updatedRequest.value
    }

    def mdtpdiSetCookie(result: Result): Cookie =
      result.newCookies.find(_.name == DeviceId.MdtpDeviceId).value

    def expectAuditIdEvent(badCookie: String, validCookie: String) = {
      val captor = ArgCaptor[DataEvent]
      verify(filter.auditConnector).sendEvent(captor)(any, any)
      val event = captor.value

      event.auditType   shouldBe EventTypes.Failed
      event.auditSource shouldBe "SomeAppName"

      event.detail should contain("tamperedDeviceId" -> badCookie)
      event.detail should contain("deviceID"         -> validCookie)
    }

    def invokeFilter(filter: DeviceIdFilter)(cookies: Seq[Cookie], expectedResultCookie: Cookie) = {
      val incomingRequest = FakeRequest().withCookies(cookies: _*)

      val result = filter(action)(incomingRequest).futureValue

      val expectedCookie =
        requestPassedToAction()
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
        reset(action)
        when(action.apply(any)).thenReturn(Future.successful(resultFromAction))

        val uuid                    = filter.generateUUID
        val timestamp               = filter.getTimeStamp
        val deviceIdMadeFromPrevKey = DeviceId(uuid, timestamp, DeviceId.generateHash(uuid, timestamp, prevSecret))
        val cookie                  = filter.makeCookie(deviceIdMadeFromPrevKey)

        val result = invokeFilter(filter)(Seq(cookie), cookie)

        val responseCookie = mdtpdiSetCookie(result)
        responseCookie.value  shouldBe deviceIdMadeFromPrevKey.value
        responseCookie.secure shouldBe true
      }
    }
  }

  "During request pre-processing, the filter" should {
    "create a new deviceId if the deviceId cookie received contains an empty value " in new Setup {
      val result = invokeFilter(filter)(Seq(newFormatGoodCookieDeviceId.copy(value = "")), newFormatGoodCookieDeviceId)

      val responseCookie = mdtpdiSetCookie(result)
      responseCookie.value  shouldBe newFormatGoodCookieDeviceId.value
      responseCookie.secure shouldBe true
    }

    "create new deviceId cookie when no cookies exists" in new Setup {
      val result = invokeFilter(filter)(Seq.empty, newFormatGoodCookieDeviceId)

      val responseCookie = mdtpdiSetCookie(result)
      responseCookie.value  shouldBe newFormatGoodCookieDeviceId.value
      responseCookie.secure shouldBe true
    }

    "not change the request or the response when a valid new format mdtpdi cookie exists" in new Setup {
      val result = invokeFilter(filter)(Seq(newFormatGoodCookieDeviceId, normalCookie), newFormatGoodCookieDeviceId)

      val expectedCookie1 = requestPassedToAction().cookies.get("AnotherCookie1").get
      val expectedCookie2 = requestPassedToAction().cookies.get(DeviceId.MdtpDeviceId).get

      expectedCookie1.value shouldBe "normalValue1"
      expectedCookie2.value shouldBe newFormatGoodCookieDeviceId.value

      val responseCookie = mdtpdiSetCookie(result)
      responseCookie.value  shouldBe newFormatGoodCookieDeviceId.value
      responseCookie.secure shouldBe true
    }

    "respect mdtpdi cookie secure setting, keeping the same value - starting with secure=false" in new Setup {
      override lazy val filter = makeFilter(secureCookie = true)

      val result =
        invokeFilter(filter)(Seq(newFormatGoodCookieDeviceId, normalCookie), newFormatGoodCookieDeviceId.copy(secure = false))

      val expectedCookie1 = requestPassedToAction().cookies.get("AnotherCookie1").get
      val expectedCookie2 = requestPassedToAction().cookies.get(DeviceId.MdtpDeviceId).get

      expectedCookie1.value shouldBe "normalValue1"
      expectedCookie2.value shouldBe newFormatGoodCookieDeviceId.value

      val responseCookie = mdtpdiSetCookie(result)
      responseCookie.value  shouldBe newFormatGoodCookieDeviceId.value
      responseCookie.secure shouldBe true
    }

    "respect mdtpdi cookie secure setting, keeping the same value - starting with secure=true" in new Setup {
      override lazy val filter = makeFilter(secureCookie = false)

      val result =
        invokeFilter(filter)(Seq(newFormatGoodCookieDeviceId, normalCookie), newFormatGoodCookieDeviceId.copy(secure = true))

      val expectedCookie1 = requestPassedToAction().cookies.get("AnotherCookie1").get
      val expectedCookie2 = requestPassedToAction().cookies.get(DeviceId.MdtpDeviceId).get

      expectedCookie1.value shouldBe "normalValue1"
      expectedCookie2.value shouldBe newFormatGoodCookieDeviceId.value

      val responseCookie = mdtpdiSetCookie(result)
      responseCookie.value  shouldBe newFormatGoodCookieDeviceId.value
      responseCookie.secure shouldBe false
    }

    "identify new format deviceId cookie has invalid hash and create new deviceId cookie" in new Setup {
      val newFormatBadCookieDeviceId = {
        val deviceId = filter.generateDeviceId().copy(hash = "wrongvalue")
        Cookie(DeviceId.MdtpDeviceId, deviceId.value, Some(DeviceId.TenYears))
      }

      val result = invokeFilter(filter)(Seq(newFormatBadCookieDeviceId), newFormatGoodCookieDeviceId)

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

      val result = invokeFilter(filter)(Seq(newFormatBadCookieDeviceId), newFormatGoodCookieDeviceId)

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

      val result = invokeFilter(filter)(Seq(newFormatBadCookieDeviceId), newFormatGoodCookieDeviceId)

      val responseCookie = mdtpdiSetCookie(result)
      responseCookie.value  shouldBe newFormatGoodCookieDeviceId.value
      responseCookie.secure shouldBe true
    }
  }
}
