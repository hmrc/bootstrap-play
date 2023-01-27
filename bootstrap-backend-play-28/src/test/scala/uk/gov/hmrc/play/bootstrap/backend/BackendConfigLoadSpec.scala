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

package uk.gov.hmrc.play.bootstrap.backend

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api._
import play.api.http.HttpConfiguration
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{ApplicationLifecycle, ConfigurationProvider}
import play.api.libs.Files.TemporaryFileCreator
import play.api.libs.crypto._
import play.api.routing.Router

import scala.concurrent.ExecutionContext

class BackendConfigLoadSpec extends AnyWordSpecLike with Matchers {
  "config loading" should {
    "load config correctly" in {
      val app = new GuiceApplicationBuilder()
        .configure(Configuration(ConfigFactory.load("backend.conf").withoutPath("play.filters.enabled")))
        .build()
      val injector = app.injector

      // todo (konrad)    verify what we actually need, and what about CryptoConfig
      // todo (konrad)    this test doesn't seem very useful

      injector.instanceOf[MessagesApi]           should not be (null)
      injector.instanceOf[Environment]           should not be (null)
      injector.instanceOf[ConfigurationProvider] should not be (null)
      injector.instanceOf[Configuration]         should not be (null)
      injector.instanceOf[HttpConfiguration]     should not be (null)
      injector.instanceOf[ApplicationLifecycle]  should not be (null)
      injector.instanceOf[Router]                should not be (null)
      injector.instanceOf[ActorSystem]           should not be (null)
      injector.instanceOf[Materializer]          should not be (null)
      injector.instanceOf[ExecutionContext]      should not be (null)
//      injector.instanceOf[CryptoConfig]          should not be (null)
      injector.instanceOf[CookieSigner]         should not be (null)
      injector.instanceOf[CSRFTokenSigner]      should not be (null)
      injector.instanceOf[TemporaryFileCreator] should not be (null)
    }
  }
}
