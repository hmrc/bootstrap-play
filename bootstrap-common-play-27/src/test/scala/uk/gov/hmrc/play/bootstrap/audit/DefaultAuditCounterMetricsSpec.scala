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

package uk.gov.hmrc.play.bootstrap.audit

import com.codahale.metrics.{MetricFilter, MetricRegistry}
import com.kenshoo.play.metrics.{DisabledMetrics, Metrics}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DefaultAuditCounterMetricsSpec
  extends AnyWordSpec
    with Matchers {

  "DefaultAuditCounterMetrics" should {

    "map Some(long) to long" in {
      valueFor(Some(0)) shouldBe 0
      valueFor(Some(5000)) shouldBe 5000
    }

    "map None to null" in {
      valueFor(None) shouldBe (null)
    }

    "tolerate disabled metrics" in {
      val metrics = new DefaultAuditCounterMetrics(new DisabledMetrics)
      metrics.registerMetric("mymetric", () => Some(1))
    }
  }

  private def valueFor(input: Option[Long]): AnyRef = {
    val registry = new MetricRegistry
    val stubMetrics = new Metrics {
      override def defaultRegistry: MetricRegistry = registry

      override def toJson: String = ???
    }
    val counterMetrics = new DefaultAuditCounterMetrics(stubMetrics)
    counterMetrics.registerMetric("mymetric", () => input)
    registry.getGauges(MetricFilter.ALL).get("mymetric").getValue.asInstanceOf[AnyRef]
  }
}
