/*
 * Copyright 2020 HM Revenue & Customs
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

import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.{verify, verifyNoMoreInteractions, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import play.api.http.{HttpConfiguration, HttpFilters}
import play.api.mvc.{Handler, RequestHeader}
import play.api.routing.Router
import play.api.test.FakeRequest
import org.mockito.Mockito.times

class RequestHandlerSpec extends WordSpec with Matchers with MockitoSugar {

  "Routing requests" should {
    "try passing a request with trailing slash removed if handler was not found" in new Setup {
      val request        = FakeRequest("GET", "path/")
      val requestHandler = new RequestHandler(mockedRouter, null, configuration, filters)

      when(mockedRouter.handlerFor(any())).thenReturn(None)
      val _ = requestHandler.routeRequest(request)

      val requestCaptor = ArgumentCaptor.forClass(classOf[RequestHeader])

      verify(mockedRouter, times(2)).handlerFor(requestCaptor.capture())
      requestCaptor.getAllValues.get(0).path shouldBe "path/"
      requestCaptor.getAllValues.get(1).path shouldBe "path"
    }

    "not modify request if handler was found" in new Setup {
      val request          = FakeRequest("GET", "path/")
      val requestHandler   = new RequestHandler(mockedRouter, null, configuration, filters)
      val handler: Handler = new Handler {}

      when(mockedRouter.handlerFor(request)).thenReturn(Some(handler))
      requestHandler.routeRequest(request) shouldBe Some(handler)

      verify(mockedRouter).handlerFor(request)
      verifyNoMoreInteractions(mockedRouter)
    }

  }

  trait Setup {
    val mockedRouter = mock[Router]

    val configuration = mock[HttpConfiguration]
    when(configuration.context).thenReturn("context")

    val filters = mock[HttpFilters]
    when(filters.filters).thenReturn(Seq.empty)
  }
}
