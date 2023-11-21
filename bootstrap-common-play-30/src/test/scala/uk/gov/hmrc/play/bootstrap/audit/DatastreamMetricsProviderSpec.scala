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

package uk.gov.hmrc.play.bootstrap.audit

import com.codahale.metrics.{Counter, MetricRegistry}
import uk.gov.hmrc.play.bootstrap.metrics.Metrics
import org.mockito.Strictness
import org.mockito.scalatest.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.slf4j.Logger
import uk.gov.hmrc.play.bootstrap.graphite.GraphiteReporterProviderConfig
import uk.gov.hmrc.play.audit.http.connector.DatastreamMetrics

import java.util.concurrent.TimeUnit

class DatastreamMetricsProviderSpec
  extends AnyWordSpec
     with Matchers
     with MockitoSugar {

  "DisabledDatastreamMetricsProvider" should {
    class TestDisabledDatastreamMetricsProvider extends DisabledDatastreamMetricsProvider {
      override val logger = mock[Logger](withSettings.strictness(Strictness.Lenient))
    }

    "return a fully-disabled DatastreamMetrics" in {
      new TestDisabledDatastreamMetricsProvider().get() shouldBe DatastreamMetrics.disabled
    }

    "log a warning" in {
      val provider = new TestDisabledDatastreamMetricsProvider()
      provider.get()

      verify(provider.logger).warn("DatastreamMetrics have been disabled since metrics are disabled: ENABLE IN ALL TESTING + PRODUCTION ENVIRONMENTS")
    }
  }

  "EnabledDatastreamMetricsProvider" should {
    "register the correct metrics key and counters" in {
      val configuration  = GraphiteReporterProviderConfig(prefix = "play.service", TimeUnit.MILLISECONDS, TimeUnit.SECONDS)
      val metricRegistry = mock[MetricRegistry]
      val metrics        = mock[Metrics]
      val successCounter = mock[Counter]
      val rejectCounter  = mock[Counter]
      val failureCounter = mock[Counter]

      when(metrics.defaultRegistry).thenReturn(metricRegistry)
      when(metricRegistry.counter("audit.success")).thenReturn(successCounter)
      when(metricRegistry.counter("audit.reject")).thenReturn(rejectCounter)
      when(metricRegistry.counter("audit.failure")).thenReturn(failureCounter)

      val datastreamMetrics = new EnabledDatastreamMetricsProvider(configuration, metrics).get

      datastreamMetrics.metricsKey shouldBe Some("play.service")

      datastreamMetrics.successCounter.inc()
      verify(successCounter).inc()

      datastreamMetrics.failureCounter.inc()
      verify(failureCounter).inc()

      datastreamMetrics.rejectCounter.inc()
      verify(rejectCounter).inc()
    }
  }
}
