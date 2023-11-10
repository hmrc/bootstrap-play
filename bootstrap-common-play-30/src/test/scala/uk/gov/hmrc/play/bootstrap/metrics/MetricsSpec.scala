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
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._

import scala.jdk.CollectionConverters._

class MetricsSpec extends AnyWordSpec with Matchers {

  def withApplication[T](conf: Map[String, Any])(block: Application => T): T = {

    lazy val application = new GuiceApplicationBuilder()
      .configure(conf)
      .overrides(
        bind[Metrics].to[MetricsImpl]
      ).build()

    running(application){block(application)}
  }

  def metrics(implicit app: Application) = app.injector.instanceOf[Metrics]

  "Metrics" should {
    "contain JVM metrics" in withApplication(Map("metrics.jvm" -> true)) { implicit app =>
      metrics.defaultRegistry.getGauges.asScala should contain key "jvm.attribute.name"
    }

    "contain logback metrics" in withApplication(Map.empty) { implicit app =>
      metrics.defaultRegistry.getMeters.asScala should contain key "ch.qos.logback.core.Appender.all"
    }

    "be able to turn off JVM metrics" in withApplication(Map("metrics.jvm" -> false)) { implicit app =>
      metrics.defaultRegistry.getGauges.asScala shouldNot contain key "jvm.attribute.name"
    }

    "be able to turn off logback metrics" in withApplication(Map("metrics.logback" -> false)) { implicit app =>
      metrics.defaultRegistry.getMeters.asScala shouldNot contain key "ch.qos.logback.core.Appender.all"
    }
  }
}
