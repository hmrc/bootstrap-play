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

package uk.gov.hmrc.play.bootstrap.metrics

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers._


class MetricsControllerSpec extends AnyWordSpec with Matchers {

  "MetricsController" should {
    "return JSON serialized by Metric's toJson with correct headers" in {
      val controller = new MetricsController(new Metrics {
        def defaultRegistry = throw new NotImplementedError
        def toJson = "{}"
      }, Helpers.stubControllerComponents())

      val result = controller.metrics.apply(FakeRequest())
      contentAsString(result) shouldBe "{}"
      contentType(result) shouldBe Some("application/json")
      headers(result) should contain value "must-revalidate,no-cache,no-store"
    }

    "return 500 if metrics module is disabled" in {

      val controller = new MetricsController(new DisabledMetrics(), Helpers.stubControllerComponents())

      val result = controller.metrics.apply(FakeRequest())

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
