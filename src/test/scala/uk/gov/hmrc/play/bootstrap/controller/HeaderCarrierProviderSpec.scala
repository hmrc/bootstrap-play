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

package uk.gov.hmrc.play.bootstrap.controller

import org.scalatest.Matchers._
import org.scalatest.WordSpec
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions

class HeaderCarrierProviderSpec extends WordSpec with TableDrivenPropertyChecks {

  private val backendHeaderCarrierProvider = new BackendHeaderCarrierProvider with (RequestHeader => HeaderCarrier) {
    def apply(request: RequestHeader): HeaderCarrier = hc(request)
  }

  private val frontendHeaderCarrierProvider = new FrontendHeaderCarrierProvider with (RequestHeader => HeaderCarrier) {
    def apply(request: RequestHeader): HeaderCarrier = hc(request)
  }

  private val scenarios = Table(
    "provider name"                        -> "provider",
    classOf[BackendHeaderCarrierProvider]  -> backendHeaderCarrierProvider,
    classOf[FrontendHeaderCarrierProvider] -> frontendHeaderCarrierProvider
  )

  forAll(scenarios) { (providerClass, headerCarrierProvider) =>
    providerClass.getSimpleName should {
      "create HeaderCarrier with path added to tags" in {
        val request       = FakeRequest(GET, "/the/request/path")
        val headerCarrier = headerCarrierProvider(request)
        val tags          = new AuditExtensions.AuditHeaderCarrier(headerCarrier).toAuditTags()

        tags.get("path") shouldBe Some("/the/request/path")
      }
    }
  }
}
