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

package uk.gov.hmrc.play.bootstrap.config

import java.io.File

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import play.api.http.HeaderNames
import play.api.mvc.Result
import play.api.{Configuration, Environment, Mode}
import play.test.WithApplication

class AuthRedirectsSpec extends WordSpec with ScalaFutures with Matchers {

  trait Dev {
    val mode = Mode.Dev
  }

  trait Prod {
    val mode = Mode.Prod
  }

  trait BaseUri {
    val ggLoginService = "http://localhost:9025"
    val ggLoginPath    = "/gg/sign-in"

    val ivService = "http://localhost:9938"
    val ivPath    = "/mdtp/uplift"

    val strideService = "http://localhost:9041"
    val stridePath    = "/stride/sign-in"
  }

  trait Setup extends WithApplication with BaseUri {

    def mode: Mode

    def extraConfig: Map[String, Any] = Map()

    trait TestRedirects extends AuthRedirects {

      val env = Environment(new File("."), getClass.getClassLoader, mode)

      val config = Configuration.from(
        Map(
          "appName"  -> "app",
          "run.mode" -> mode.toString
        ) ++ extraConfig)
    }

    object Redirect extends TestRedirects

    def expectedLocation: String

    def validate(redirect: Result): Unit = {
      redirect.header.status                        shouldBe 303
      redirect.header.headers(HeaderNames.LOCATION) shouldBe expectedLocation
    }
  }

  "redirect with defaults from config" should {

    "redirect to GG login in Dev" in new Setup with Dev {

      val expectedLocation = s"$ggLoginService$ggLoginPath?continue=%2Fcontinue&origin=app"

      validate(Redirect.toGGLogin("/continue"))

    }

    "redirect to GG login in Prod" in new Setup with Prod {

      val expectedLocation = s"$ggLoginPath?continue=%2Fcontinue&origin=app"

      validate(Redirect.toGGLogin("/continue"))

    }

    "redirect to stride auth in Dev without failureURL" in new Setup with Dev {

      val expectedLocation = s"$strideService$stridePath?successURL=%2Fsuccess&origin=app"

      validate(Redirect.toStrideLogin("/success"))

    }

    "redirect to stride auth in Dev with failureURL" in new Setup with Dev {

      val expectedLocation = s"$strideService$stridePath?successURL=%2Fsuccess&origin=app&failureURL=%2Ffailure"

      validate(Redirect.toStrideLogin("/success", Some("/failure")))

    }

    "redirect to stride auth in Prod without failureURL" in new Setup with Prod {

      val expectedLocation = s"$stridePath?successURL=%2Fsuccess&origin=app"

      validate(Redirect.toStrideLogin("/success"))

    }

    "redirect to stride auth in Prod with failureURL" in new Setup with Prod {

      val expectedLocation = s"$stridePath?successURL=%2Fsuccess&origin=app&failureURL=%2Ffailure"

      validate(Redirect.toStrideLogin("/success", Some("/failure")))

    }

    "allow to override the host defaults" in new Setup with Dev {

      override def extraConfig = Map("Dev.external-url.company-auth-frontend.host" -> "http://localhost:9999")

      val expectedLocation = s"http://localhost:9999$ggLoginPath?continue=%2Fcontinue&origin=app"

      validate(Redirect.toGGLogin("/continue"))

    }

    "allow to override the origin default in configuration" in new Setup with Dev {

      override def extraConfig = Map("sosOrigin" -> "customOrigin")

      val expectedLocation = s"$ggLoginService$ggLoginPath?continue=%2Fcontinue&origin=customOrigin"

      validate(Redirect.toGGLogin("/continue"))

    }

    "allow to override the origin default in code" in new Setup with Dev {

      object CustomRedirect extends TestRedirects {
        override val origin = "customOrigin"
      }

      val expectedLocation = s"$ggLoginService$ggLoginPath?continue=%2Fcontinue&origin=customOrigin"

      validate(CustomRedirect.toGGLogin("/continue"))

    }

  }

}
