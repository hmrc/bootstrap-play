/*
 * Copyright 2023 HM Revenue & Customs
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

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.{BeforeAndAfterAll, OptionValues}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc._
import play.api.mvc.request.RequestAttrKey
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
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
  lazy val uuid      = java.util.UUID.randomUUID().toString

  implicit val system: ActorSystem =
    ActorSystem("DeviceIdFilterSpec")

  override def afterAll(): Unit = {
    system.terminate()
  }

  private trait Setup extends Results {
    val normalCookie = Cookie("AnotherCookie1", "normalValue1")

    val resultFromAction: Result = Ok

    lazy val action = {
      val mockAction       = mock[(RequestHeader) => Future[Result]]
      val outgoingResponse = Future.successful(resultFromAction)
      when(mockAction.apply(any[RequestHeader]))
        .thenReturn(outgoingResponse)
      mockAction
    }

    lazy val mockAuditConnector = mock[AuditConnector]

    def makeFilter(secureCookie: Boolean): DeviceIdFilter = new DeviceIdFilter {

      override def getTimeStamp() = timestamp
      override def generateUUID(): String = uuid


      override val mat             = implicitly[Materializer]

      override val secret          = "SOME_SECRET"
      override val previousSecrets = Seq("previous_key_1", "previous_key_2")
      override val secure          = secureCookie

      override val appName         = "SomeAppName"

      override val auditConnector = mockAuditConnector

      override protected implicit def ec: ExecutionContext = ExecutionContext.global
    }


    lazy val filter = makeFilter(secureCookie = true)

    lazy val newFormatGoodCookieDeviceId = filter.buildNewDeviceIdCookie()

    def requestPassedToAction(): RequestHeader = {
      val updatedRequest = ArgumentCaptor.forClass(classOf[RequestHeader])
      verify(action, times(1)).apply(updatedRequest.capture())
      updatedRequest.getValue
    }

    def mdtpdiSetCookie(result: Result): Cookie =
      result.newCookies.find(_.name == DeviceId.MdtpDeviceId).value

    def expectAuditIdEvent(badCookie: String, validCookie: String) = {
      val captor = ArgumentCaptor.forClass(classOf[DataEvent])
      verify(mockAuditConnector).sendEvent(captor.capture())(any[HeaderCarrier], any[ExecutionContext])
      val event = captor.getValue

      event.auditType   shouldBe EventTypes.Failed
      event.auditSource shouldBe "SomeAppName"

      event.detail should contain("tamperedDeviceId" -> badCookie)
      event.detail should contain("deviceID"         -> validCookie)
    }

    def invokeFilter(filter: DeviceIdFilter)(withCookies: Seq[Cookie], expectedResultCookie: Cookie) = {
      val incomingRequest = FakeRequest().withCookies(withCookies: _*)

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
        when(action.apply(any[RequestHeader]))
          .thenReturn(Future.successful(resultFromAction))

        val uuid                    = filter.generateUUID()
        val timestamp               = filter.getTimeStamp()
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
      val result =
        invokeFilter(filter)(
          withCookies          = Seq(newFormatGoodCookieDeviceId.copy(value = "")),
          expectedResultCookie = newFormatGoodCookieDeviceId
        )

      val responseCookie = mdtpdiSetCookie(result)
      responseCookie.value  shouldBe newFormatGoodCookieDeviceId.value
      responseCookie.secure shouldBe true
    }

    "create new deviceId cookie when no cookies exists" in new Setup {
      val result =
        invokeFilter(filter)(
          withCookies          = Seq.empty,
          expectedResultCookie = newFormatGoodCookieDeviceId
        )

      val responseCookie = mdtpdiSetCookie(result)
      responseCookie.value  shouldBe newFormatGoodCookieDeviceId.value
      responseCookie.secure shouldBe true
    }

    "not change the request or the response when a valid new format mdtpdi cookie exists" in new Setup {
      val result =
        invokeFilter(filter)(
          withCookies          = Seq(newFormatGoodCookieDeviceId, normalCookie),
          expectedResultCookie = newFormatGoodCookieDeviceId
        )

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
        invokeFilter(filter)(
          withCookies          = Seq(newFormatGoodCookieDeviceId, normalCookie),
          expectedResultCookie = newFormatGoodCookieDeviceId.copy(secure = false)
        )

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
        invokeFilter(filter)(
          withCookies          = Seq(newFormatGoodCookieDeviceId, normalCookie),
          expectedResultCookie = newFormatGoodCookieDeviceId.copy(secure = true)
        )

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

      val result =
        invokeFilter(filter)(
          withCookies          = Seq(newFormatBadCookieDeviceId),
          expectedResultCookie = newFormatGoodCookieDeviceId
        )

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

      val result =
        invokeFilter(filter)(
          withCookies          = Seq(newFormatBadCookieDeviceId),
          expectedResultCookie = newFormatGoodCookieDeviceId
        )

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
          Some(DeviceId.TenYears)
        )
      }

      val result =
        invokeFilter(filter)(
          withCookies          = Seq(newFormatBadCookieDeviceId),
          expectedResultCookie = newFormatGoodCookieDeviceId
        )

      val responseCookie = mdtpdiSetCookie(result)
      responseCookie.value  shouldBe newFormatGoodCookieDeviceId.value
      responseCookie.secure shouldBe true
    }
  }
}
