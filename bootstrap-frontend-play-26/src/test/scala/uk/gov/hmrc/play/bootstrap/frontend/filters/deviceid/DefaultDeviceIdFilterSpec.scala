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

import javax.inject.Inject
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.{DefaultHttpFilters, HttpFilters}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.frontend.filters.deviceid.DeviceId.MdtpDeviceId

class DefaultDeviceIdFilterSpec
    extends AnyWordSpecLike
    with Matchers
    with MockitoSugar
    with OptionValues
    with ScalaFutures {

  import DefaultDeviceIdFilterSpec._

  "DeviceIdFilter" should {

    "create the deviceId when no cookie exists" in {
      running(application(having = appConfig)) { application =>
        val Some(result) = route(application, FakeRequest(GET, "/test"))
        cookies(result).get(DeviceId.MdtpDeviceId) shouldBe 'defined
      }
    }

    "create the deviceId when no cookie exists and previous keys are empty" in {
      running(application(having = appConfigNoPreviousKey)) { application =>
        val Some(result) = route(application, FakeRequest(GET, "/test"))
        cookies(result).get(DeviceId.MdtpDeviceId) shouldBe 'defined
      }
    }

    "do nothing when a valid cookie exists" in {

      running(application(having = appConfig)) { application =>
        val existingCookie = createDeviceId.buildNewDeviceIdCookie()

        val Some(result) = route(
          application,
          FakeRequest(GET, "/test").withCookies(existingCookie)
        )

        cookies(result) should contain(existingCookie)
      }
    }

    "successfully decode a deviceId generated from a previous secret" in {
      running(application(having = appConfig)) { application =>
        val uuid = createDeviceId.generateUUID

        val existingCookie = {
          val timestamp = createDeviceId.getTimeStamp
          val deviceIdMadeFromPrevKey =
            DeviceId(uuid, timestamp, DeviceId.generateHash(uuid, timestamp, thePreviousSecret))
          createDeviceId.makeCookie(deviceIdMadeFromPrevKey)
        }

        val Some(result) = route(
          application,
          FakeRequest(GET, "/test").withCookies(existingCookie)
        )

        val Some(mdtpidCookieValue) = cookies(result).get(MdtpDeviceId).map(_.value)
        mdtpidCookieValue should include(uuid)
      }
    }
  }

  private val theSecret         = "some_secret"
  private val thePreviousSecret = "some previous secret with spaces since spaces cause an issue unless encoded!!!"

  private val createDeviceId = new DeviceIdCookie {
    override val secret          = theSecret
    override val previousSecrets = Seq(thePreviousSecret)
  }

  private val appConfigNoPreviousKey: Map[String, Any] = Map("cookie.deviceId.secret" -> theSecret)
  private val appConfig: Map[String, Any] = appConfigNoPreviousKey + ("cookie.deviceId.previous.secret" -> Seq(
    thePreviousSecret))

  private def application(having: Map[String, Any]): GuiceApplicationBuilder => GuiceApplicationBuilder =
    applicationBuilder => {

      import play.api.inject._

      applicationBuilder
        .bindings(
          bind[String].qualifiedWith("appName").toInstance("myApp"),
          bind[DeviceIdFilter].to[DefaultDeviceIdFilter],
          bind[AuditConnector].toInstance(mock[AuditConnector])
        )
        .overrides(
          bind[HttpFilters].to[Filters]
        )
        .configure(having)
    }
}

object DefaultDeviceIdFilterSpec {
  class Filters @Inject()(deviceId: DeviceIdFilter) extends DefaultHttpFilters(deviceId)
}
