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

package uk.gov.hmrc.play.bootstrap.http

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.TestData
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.http.utils._

import scala.concurrent.ExecutionContext

class DefaultHttpClientSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with WiremockTestServer
    with GuiceOneAppPerTest {

  private val appName = "myApp"

  import ExecutionContext.Implicits.global
  import HttpReads.Implicits._

  override def newAppForTest(testData: TestData): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[String].qualifiedWith("appName").toInstance(appName),
        bind[HttpAuditing].to[DefaultHttpAuditing],
        bind[uk.gov.hmrc.http.HttpClient].to[DefaultHttpClient],
        bind[AuditConnector].toInstance(new TestAuditConnector(appName))
      )
      .build()

  def myHttpClient = app.injector.instanceOf[uk.gov.hmrc.http.HttpClient]

  "A GET" should {

    implicit val hc    = HeaderCarrier()
    implicit val reads = BankHolidays.reads

    "read some json and return a case class" in {
      stubFor(
        get("/bank-holidays.json")
          .willReturn(ok(JsonPayloads.bankHolidays)))

      val bankHolidays: BankHolidays =
        myHttpClient.GET[BankHolidays](s"http://localhost:$wireMockPort/bank-holidays.json").futureValue
      bankHolidays.events.head shouldBe BankHoliday("New Year's Day")
    }

    "read some json and return a raw http response" in {
      stubFor(
        get("/bank-holidays.json")
          .willReturn(ok(JsonPayloads.bankHolidays)))

      val response: HttpResponse = myHttpClient.GET[HttpResponse](s"http://localhost:$wireMockPort/bank-holidays.json").futureValue
      response.status shouldBe 200
      response.body   shouldBe JsonPayloads.bankHolidays
    }

    "be able to handle a 404 without throwing an exception" in {
      stubFor(
        get("/404.json")
          .willReturn(notFound))

      // By adding an Option to your case class, the 404 is translated into None
      val bankHolidays: Option[BankHolidays] =
        myHttpClient.GET[Option[BankHolidays]](s"http://localhost:$wireMockPort/404.json").futureValue
      bankHolidays shouldBe None
    }

    "be able to handle an empty body on 204" in {
      stubFor(
        get("/204.json")
          .willReturn(noContent))

      val bankHolidays = myHttpClient.GET[Unit](s"http://localhost:$wireMockPort/204.json").futureValue
      bankHolidays shouldBe (())
    }

    "throw an BadRequestException for 400 errors" in {
      stubFor(
        get("/400.json")
          .willReturn(badRequest))

      myHttpClient
        .GET[Option[BankHolidays]](s"http://localhost:$wireMockPort/400.json")
        .recover {
          case UpstreamErrorResponse.WithStatusCode(400) => // handle here a bad request
        }
        .futureValue
    }

    "throw an Upstream4xxResponse for 4xx errors" in {
      stubFor(
        get("/401.json")
          .willReturn(unauthorized))

      myHttpClient
        .GET[Option[BankHolidays]](s"http://localhost:$wireMockPort/401.json")
        .recover {
          case UpstreamErrorResponse.Upstream4xxResponse(e) => // handle here a 4xx errors
        }
        .futureValue
    }

    "throw an Upstream5xxResponse for 4xx errors" in {
      stubFor(
        get("/500.json")
          .willReturn(serverError))

      myHttpClient
        .GET[Option[BankHolidays]](s"http://localhost:$wireMockPort/500.json")
        .recover {
          case UpstreamErrorResponse.Upstream5xxResponse(e) => // handle here a 5xx errors
        }
        .futureValue
    }
  }

  "A POST" should {
    implicit val hc  = HeaderCarrier()
    implicit val uw  = User.writes
    implicit val uir = UserIdentifier.reads

    "write a case class to json body and return a response" in {
      stubFor(
        post("/create-user")
          .willReturn(noContent))
      val user = User("me@mail.com", "John Smith")

      // Use HttpResponse when the API always returns an empty body
      val response: HttpResponse =
        myHttpClient.POST[User, HttpResponse](s"http://localhost:$wireMockPort/create-user", user).futureValue
      response.status shouldBe 204
    }

    "read the response body of the POST into a case class" in {
      stubFor(
        post("/create-user")
          .willReturn(ok(JsonPayloads.userId)))
      val user = User("me@mail.com", "John Smith")

      // Use a case class when the API returns a json body
      val userId: UserIdentifier =
        myHttpClient.POST[User, UserIdentifier](s"http://localhost:$wireMockPort/create-user", user).futureValue
      userId.id shouldBe "123"
    }
  }
}
