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

package uk.gov.hmrc.play.bootstrap.http

import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.OptionalDevContext
import play.api.http.{HttpConfiguration, HttpErrorHandler, HttpFilters}
import play.api.mvc.{Handler, RequestHeader}
import play.api.routing.Router
import play.api.test.FakeRequest
import play.core.DefaultWebCommands

class RequestHandlerSpec extends AnyWordSpec with Matchers with MockitoSugar {

  "Routing requests" should {
    "try passing a request with trailing slash removed if handler was not found" in new Setup {
      val request = FakeRequest("GET", "path/")

      when(mockedRouter.handlerFor(any))
        .thenReturn(None)

      requestHandler.routeRequest(request)

      val requestCaptor = ArgCaptor[RequestHeader]

      verify(mockedRouter, times(2)).handlerFor(requestCaptor.capture)
      requestCaptor.values.map(_.path) shouldBe List("path/", "path")
    }

    "not modify request if handler was found" in new Setup {
      val request = FakeRequest("GET", "path/")
      val handler = new Handler {}

      when(mockedRouter.handlerFor(request))
        .thenReturn(Some(handler))

      requestHandler.routeRequest(request) shouldBe Some(handler)

      verify(mockedRouter).handlerFor(request)
      verifyNoMoreInteractions(mockedRouter)
    }
  }

  trait Setup {
    val mockedRouter = mock[Router]

    val mockedHttpErrorHandler = mock[HttpErrorHandler]

    val mockedConfiguration = mock[HttpConfiguration]
    when(mockedConfiguration.context)
      .thenReturn("context")

    val mockedFilters = mock[HttpFilters]
    when(mockedFilters.filters)
      .thenReturn(Seq.empty)

    val requestHandler =
      new RequestHandler(
        webCommands   = new DefaultWebCommands,
        optDevContext = new OptionalDevContext(None),
        router        = mockedRouter,
        errorHandler  = mockedHttpErrorHandler,
        configuration = mockedConfiguration,
        filters       = mockedFilters
      )
  }
}
