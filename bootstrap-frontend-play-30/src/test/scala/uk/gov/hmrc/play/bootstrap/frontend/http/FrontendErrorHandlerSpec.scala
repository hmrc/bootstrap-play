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

package uk.gov.hmrc.play.bootstrap.frontend.http

import org.apache.pekko.stream.Materializer
import org.scalatest.AppendedClues.convertToClueful
import org.scalatest.Inspectors.forAll
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.{GuiceOneAppPerTest, GuiceOneServerPerTest}
import play.api.Application
import play.api.http.HttpEntity
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.i18n.Messages.Attrs.CurrentLang
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.typedmap.TypedMap
import play.api.mvc._
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import play.mvc.Http.HeaderNames
import play.twirl.api.Html

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FrontendErrorHandlerSpec
  extends AnyWordSpec
     with Matchers
     with GuiceOneAppPerTest
     with ScalaFutures {

  override def fakeApplication(): Application  =
    new GuiceApplicationBuilder()
      .configure(Map("play.i18n.langs" -> List("en", "cy")))
      .build()

  implicit lazy val materializer: Materializer = app.materializer

  object TestFrontendErrorHandler extends FrontendErrorHandler {
    override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit rh: RequestHeader): Future[Html] =
      Future.successful(Html(s"$pageTitle\n$heading\n$message"))
    override def messagesApi: MessagesApi      = app.injector.instanceOf[MessagesApi]
    override val ec         : ExecutionContext = app.injector.instanceOf[ExecutionContext]
  }

  val welshRequest = FakeRequest(
    method  = "GET",
    uri     = "/",
    headers = FakeHeaders(Seq(HeaderNames.HOST -> "localhost")),
    body    = AnyContentAsEmpty,
    attrs   = TypedMap(CurrentLang -> Lang("cy"))
  )

  val englishRequest = FakeRequest(
    method  = "GET",
    uri     = "/",
    headers = FakeHeaders(Seq(HeaderNames.HOST -> "localhost")),
    body    = AnyContentAsEmpty,
    attrs   = TypedMap(CurrentLang -> Lang("en"))
  )

  "resolving a client error" should {
    "fall back to a standardErrorTemplate based result" in {
      val explicitlyHandledClientErrors = List(400, 404)
      val clientErrorsHandledByFallback = (400 to 499).diff(explicitlyHandledClientErrors)

      forAll(clientErrorsHandledByFallback) { statusCode =>
        val result = TestFrontendErrorHandler.onClientError(FakeRequest(), statusCode)

        contentAsString(result) shouldBe """Sorry, there is a problem with the service
                                           |Sorry, there is a problem with the service
                                           |Try again later.""".stripMargin
      }
    }

    "provide welsh translations for messages" in {
      val clientErrors = (400 to 499).toList

      forAll(clientErrors) { statusCode =>
        val englishResult = TestFrontendErrorHandler.onClientError(englishRequest, statusCode)
        val welshResult   = TestFrontendErrorHandler.onClientError(welshRequest, statusCode)

        for {
          englishMessage <- contentAsString(englishResult).split("\n")
          welshMessage   <- contentAsString(welshResult).split("\n")
        } englishMessage shouldNot be(welshMessage) withClue ", missing global.error translation"
      }
    }
  }

  "resolving an error" should {
    "return a generic InternalServerError result" in {
      val exception = new Exception("Runtime exception")
      val result    = TestFrontendErrorHandler.resolveError(FakeRequest(), exception).futureValue

      result.header.status  shouldBe INTERNAL_SERVER_ERROR
      result.header.headers should contain(CACHE_CONTROL -> "no-cache")
    }

    "return a generic InternalServerError result if the exception cause is null" in {
      val exception = new Exception("Runtime exception", null)
      val result    = TestFrontendErrorHandler.resolveError(FakeRequest(), exception).futureValue

      result.header.status  shouldBe INTERNAL_SERVER_ERROR
      result.header.headers should contain(CACHE_CONTROL -> "no-cache")
    }

    "return 303 (See Other) result for an application error" in {
      val responseCode = SEE_OTHER
      val location     = "http://some.test.location/page"
      val theResult    = Result(
        ResponseHeader(responseCode, Map("Location" -> location)),
        HttpEntity.NoEntity
      )

      val appException = ApplicationException(theResult, "application exception")

      val result = TestFrontendErrorHandler.resolveError(FakeRequest(), appException).futureValue

      result shouldBe theResult
    }

    "provide welsh translations for messages" in {
      val exception = new Exception("Runtime exception")
      for {
        englishMessage <- contentAsString(TestFrontendErrorHandler.resolveError(englishRequest, exception)).split("\n")
        welshMessage   <- contentAsString(TestFrontendErrorHandler.resolveError(welshRequest, exception)).split("\n")
      } englishMessage shouldNot be(welshMessage) withClue ", missing global.error translation"
    }
  }
}

class TestFrontendErrorHandlerWithMessages @Inject()(
  override val messagesApi: MessagesApi
)(implicit
  override val ec: ExecutionContext
) extends FrontendErrorHandler {
  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit rh: RequestHeader): Future[Html] =
    Future.successful(Html(Messages("key")))
}

class FrontendErrorHandlerWithMessagesSpec
  extends AnyWordSpec
     with Matchers
     with GuiceOneServerPerTest
     with ScalaFutures
     with IntegrationPatience {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(Map(
        "play.http.errorHandler" -> classOf[TestFrontendErrorHandlerWithMessages].getName
      ))
      .build()

  "resolving an error" should {
    "return 404 for invalid url" in {
      // using java.net directly to ensure that the trailing `]` is not escaped
      val connection = new java.net.URL(s"http://localhost:$port/test]").openConnection.asInstanceOf[java.net.HttpURLConnection]
      connection.connect()

      connection.getResponseCode shouldBe 400
    }
  }
}
