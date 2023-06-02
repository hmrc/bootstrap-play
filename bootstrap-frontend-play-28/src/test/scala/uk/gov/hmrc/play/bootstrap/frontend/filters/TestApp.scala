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

package uk.gov.hmrc.play.bootstrap.frontend.filters

import akka.stream.Materializer
import org.scalatest.TestSuite
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results.Ok
import play.api.mvc._
import play.api.test.Helpers._
import play.api.{Application, Configuration}

import javax.inject.{Inject,Singleton}

trait TestApp extends GuiceOneAppPerSuite {
  self: TestSuite =>

  private val Action = stubControllerComponents().actionBuilder

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .bindings(bind(classOf[AllowlistFilter]).to(classOf[TestAkamaiAllowlistFilter]))
    .configure("play.http.filters" -> "uk.gov.hmrc.play.bootstrap.frontend.filters.TestFilters",
      "bootstrap.filters.allowlist.enabled" -> "true",
      "bootstrap.filters.allowlist.ips" -> "127.0.0.1",
      "bootstrap.filters.allowlist.destination" -> "/destination",
      "bootstrap.filters.allowlist.excluded" -> "/healthcheck")
    .routes {
      case ("GET", "/destination") => Action(Ok("destination"))
      case ("GET", "/index"      ) => Action(Ok("success"))
      case ("GET", "/healthcheck") => Action(Ok("ping"))
    }
    .build()
}

@Singleton
private class TestAkamaiAllowlistFilter @Inject()(
  config: Configuration,
  override val mat: Materializer
) extends AllowlistFilter(config, mat) {
  override lazy val allowlist    : Seq[String] = Seq("127.0.0.1")
  override lazy val destination  : Call        = Call("GET", "/destination")
  override lazy val excludedPaths: Seq[Call]   = Seq(Call("GET", "/healthcheck"))
}
