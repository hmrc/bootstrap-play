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

package uk.gov.hmrc.play.bootstrap.graphite

import com.codahale.metrics.{MetricFilter, SharedMetricRegistries}
import com.kenshoo.play.metrics._
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.Configuration
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers
import uk.gov.hmrc.play.audit.http.connector.DatastreamMetrics

class GraphiteMetricsModuleSpec
    extends AnyFreeSpec
    with Matchers
    with BeforeAndAfterEach
    with ScalaCheckDrivenPropertyChecks
    with GivenWhenThen {

  override def beforeEach(): Unit = {
    super.beforeEach()
    SharedMetricRegistries.clear()
  }

  def withInjector(configuration: Configuration)(block: Injector => Any) = {
    val app = new GuiceApplicationBuilder()
      .bindings(new GraphiteMetricsModule)
      .configure(configuration)
      .build()

    Helpers.running(app)(block(app.injector))
  }

  "if 'metrics.enabled' set to false" in {
    withInjector(Configuration("metrics.enabled" -> false)) { injector =>
      Then("kenshoo metrics are disabled")
      injector.instanceOf[MetricsFilter] mustBe a[DisabledMetricsFilter]
    }
  }

  "if missing kenshoo metrics enabled, but 'microservice.metrics.graphite.enabled' missing" in {
    withInjector(Configuration("metrics.enabled" -> "true")) { injector =>
      Then("kensho metrics are enabled")
      injector.instanceOf[MetricsFilter] mustBe a[MetricsFilterImpl]
      injector.instanceOf[Metrics] mustBe a[MetricsImpl]

      Then("graphite reporting in disabled")
      injector.instanceOf[GraphiteReporting] mustBe a[DisabledGraphiteReporting]
    }
  }

  "property testing" in {
    forAll { (kenshooEnabled: Boolean, graphiteEnabled: Boolean) =>
      SharedMetricRegistries.clear()

      val configuration =
         Configuration.from(
           Map("metrics.enabled" -> kenshooEnabled) ++
             (if (graphiteEnabled)
                Map(
                  "microservice.metrics.graphite.enabled" -> true,
                  "microservice.metrics.graphite.host"    -> "test",
                  "microservice.metrics.graphite.port"    -> "9999",
                  "appName"                               -> "test"
                )
              else
                Map("microservice.metrics.graphite.enabled" -> false)
             )
        )

      withInjector(configuration) { injector =>
        if (kenshooEnabled)
          //enabled kenshoo metrics filter included
          injector.instanceOf[MetricsFilter] mustBe a[MetricsFilterImpl]

        if (!kenshooEnabled) {
          //disabled kenshoo metrics filter included
          injector.instanceOf[MetricsFilter] mustBe a[DisabledMetricsFilter]

          //there is a binding to graphite disabledMetrics
          injector.instanceOf[Metrics] mustBe a[DisabledMetrics]
        }

        //there is a binding to graphite's metricsimpl or graphitemetricsimpl
        if (kenshooEnabled) {
          injector.instanceOf[Metrics] mustBe a[MetricsImpl]
          injector.instanceOf[MetricFilter] mustEqual MetricFilter.ALL
        }

        if (kenshooEnabled && graphiteEnabled) {
          //there is an enabled graphite reporter
          injector.instanceOf[GraphiteReporting] mustBe a[EnabledGraphiteReporting]

          //there is a binding to enabled DatastreamMetrics
          injector.instanceOf[DatastreamMetrics] must not be DatastreamMetrics.disabled
        }

        if (!kenshooEnabled || !graphiteEnabled) {
          //there is a disabled graphite reporter
          injector.instanceOf[GraphiteReporting] mustBe a[DisabledGraphiteReporting]

          //there is a binding to disabled DatastreamMetrics
          injector.instanceOf[DatastreamMetrics] mustBe DatastreamMetrics.disabled
        }
      }
    }
  }
}
