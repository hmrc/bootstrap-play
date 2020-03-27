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

package uk.gov.hmrc.play.bootstrap.graphite

import java.util.concurrent.TimeUnit

import com.codahale.metrics.graphite.GraphiteReporter
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, MustMatchers, WordSpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers

class GraphiteReportingSpec extends WordSpec with MustMatchers with MockitoSugar with BeforeAndAfterEach {

  val graphite: GraphiteReporter = mock[GraphiteReporter]

  def app: GuiceApplicationBuilder = {

    import play.api.inject._

    new GuiceApplicationBuilder()
      .bindings(
        bind[GraphiteReporter].toInstance(graphite),
        bind[GraphiteReporting].to[EnabledGraphiteReporting].eagerly
      )
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(graphite)
  }

  "GraphiteReporting" must {

    "start the reporter when metrics are enabled with default interval" in {

      Helpers.running(app.build()) {
        verify(graphite).start(10L, TimeUnit.SECONDS)
      }
      verify(graphite).stop()
    }

    "start the reporter when metrics are enabled with custom interval" in {

      Helpers.running(app.configure("microservice.metrics.graphite.interval" -> "11").build()) {
        verify(graphite).start(11L, TimeUnit.SECONDS)
      }
      verify(graphite).stop()
    }
  }
}
