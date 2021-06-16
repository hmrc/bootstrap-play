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

package uk.gov.hmrc.play.bootstrap.frontend.http

import akka.stream.Materializer
import org.scalatest.Inspectors.forAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.http.HttpEntity
import play.api.i18n.MessagesApi
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html

class FrontendErrorHandlerSpec extends AnyWordSpecLike with Matchers with GuiceOneAppPerTest {

  implicit lazy val materializer: Materializer = app.materializer

  object TestFrontendErrorHandler extends FrontendErrorHandler {
    override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit rh: Request[_]): Html =
       Html(s"$pageTitle\n$heading\n$message")
    override def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  }

  import TestFrontendErrorHandler._

  "resolving a client error" should {
    "fall back to a standardErrorTemplate based result" in {
      val explicitlyHandledClientErrors = List(400, 404)
      val clientErrorsHandledByFallback = (400 to 499) diff explicitlyHandledClientErrors
      
      forAll(clientErrorsHandledByFallback) { statusCode =>
        val result = onClientError(FakeRequest(), statusCode)

        contentAsString(result) shouldBe """Sorry, there is a problem with the service
                                           |Sorry, there is a problem with the service
                                           |Try again later.""".stripMargin
      }
    }
  }

  "resolving an error" should {
    "return a generic InternalServerError result" in {
      val exception = new Exception("Runtime exception")
      val result    = resolveError(FakeRequest(), exception)

      result.header.status  shouldBe INTERNAL_SERVER_ERROR
      result.header.headers should contain(CACHE_CONTROL -> "no-cache")
    }

    "return a generic InternalServerError result if the exception cause is null" in {
      val exception = new Exception("Runtime exception", null)
      val result    = resolveError(FakeRequest(), exception)

      result.header.status  shouldBe INTERNAL_SERVER_ERROR
      result.header.headers should contain(CACHE_CONTROL -> "no-cache")
    }

    "return 303 (See Other) result for an application error" in {

      val responseCode = SEE_OTHER
      val location     = "http://some.test.location/page"
      val theResult = Result(
        ResponseHeader(responseCode, Map("Location" -> location)),
        HttpEntity.NoEntity
      )

      val appException = ApplicationException(theResult, "application exception")

      val result = resolveError(FakeRequest(), appException)

      result shouldBe theResult
    }
  }
}
