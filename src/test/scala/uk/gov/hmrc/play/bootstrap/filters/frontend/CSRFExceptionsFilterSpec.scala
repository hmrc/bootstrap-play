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

package uk.gov.hmrc.play.bootstrap.filters.frontend

import akka.stream.Materializer
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.http.HttpVerbs._
import play.api.mvc.{AnyContentAsEmpty, RequestHeader}
import play.api.test.{FakeHeaders, FakeRequest}

class CSRFExceptionsFilterSpec
    extends WordSpecLike
    with Matchers
    with ScalaFutures
    with MockitoSugar
    with GuiceOneAppPerSuite {

  private val now          = () => DateTime.now().withZone(DateTimeZone.UTC)
  private val csrfTokenKey = "Csrf-Token"

  private def csrfToken(rh: RequestHeader): Option[String] = rh.headers.get(csrfTokenKey)

  def mat: Materializer = app.injector.instanceOf[Materializer]

  "CSRF exceptions filter" should {

    "do nothing if POST request and not in whitelist" in {
      val rh     = FakeRequest(POST, "/something", FakeHeaders(), AnyContentAsEmpty).withHeaders(csrfTokenKey -> "token")
      val config = mock[Configuration]
      when(config.getOptional[Seq[String]]("csrfexceptions.whitelist")).thenReturn(None)

      val filter = new CSRFExceptionsFilter(config, mat)

      csrfToken(filter.filteredHeaders(rh)) shouldBe Some("token")
    }

    "do nothing for GET requests" in {
      val rh     = FakeRequest(GET, "/ida/login", FakeHeaders(), AnyContentAsEmpty).withHeaders(csrfTokenKey -> "token")
      val config = mock[Configuration]
      when(config.getOptional[Seq[String]]("csrfexceptions.whitelist")).thenReturn(Some(Seq("/ida/login")))
      val filter = new CSRFExceptionsFilter(config, mat)

      csrfToken(filter.filteredHeaders(rh)) shouldBe Some("token")
    }

    "add Csrf-Token header with value nocheck to bypass validation for white-listed POST request" in {
      val rh     = FakeRequest(POST, "/ida/login", FakeHeaders(), AnyContentAsEmpty)
      val config = mock[Configuration]
      when(config.getOptional[Seq[String]]("csrfexceptions.whitelist")).thenReturn(Some(Seq("/ida/login")))
      val filter = new CSRFExceptionsFilter(config, mat)

      csrfToken(filter.filteredHeaders(rh)) shouldBe Some("nocheck")
    }
  }
}
