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
import com.typesafe.config.ConfigException
import org.mockito.scalatest.MockitoSugar
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Call, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future

class AllowlistFilterSpec
  extends AnyWordSpecLike
     with Matchers
     with ScalaCheckDrivenPropertyChecks
     with MockitoSugar {


  val exclusion = "/some-excluded-path"
  val healthCheckPath = "bootstrap.filters.allowlist.excludedHealthCheckPath" -> "/ping/ping"
  val nonAllowedIpAddress = "10.0.0.1"
  val govUkUrl = "https://www.gov.uk"
  val allowedIpAddress = "192.168.2.1"
  val mockMaterializer = mock[Materializer]

  val otherConfigGen = Gen.mapOf[String, String](
    for {
      key   <- Gen.alphaNumStr.suchThat(_.nonEmpty)
      value <- arbitrary[String]
    } yield (key, value)
  )

  "the list of allowlisted IP addresses" should {

    "throw an exception" when {

      "the underlying config value is not there" in {

        forAll(otherConfigGen, arbitrary[String], arbitrary[String]) {
          (otherConfig, destination, excluded) =>

            whenever(!otherConfig.contains("bootstrap.filters.allowlist.ips")) {

              val config = Configuration(
                (otherConfig +
                  ("bootstrap.filters.allowlist.destination" -> destination) +
                  ("bootstrap.filters.allowlist.excluded"    -> excluded) +
                  ("bootstrap.filters.allowlist.enabled"     -> true) +
                  healthCheckPath
                ).toSeq: _*
              )

              assertThrows[ConfigException] {
                new AllowlistFilter(config, mockMaterializer).loadConfig
              }
            }
        }
      }
    }

    "be empty" when {

      "the underlying config value is empty" in {

        forAll(otherConfigGen, arbitrary[String], arbitrary[String]) {
          (otherConfig, destination, excluded) =>

            val config = Configuration(
              (otherConfig +
                ("bootstrap.filters.allowlist.destination" -> destination) +
                ("bootstrap.filters.allowlist.excluded"    -> excluded) +
                ("bootstrap.filters.allowlist.ips"         -> "") +
                ("bootstrap.filters.allowlist.enabled"     -> true) +
                healthCheckPath
              ).toSeq: _*
            )

            val allowlistFilter = new AllowlistFilter(config, mockMaterializer)

            allowlistFilter.allowlist shouldBe empty
          }
      }
    }

    "contain all of the values" when {

      "given a comma-separated list of values" in {

        val gen = Gen.nonEmptyListOf(Gen.alphaNumStr suchThat (_.nonEmpty))

        forAll(gen, otherConfigGen, arbitrary[String], arbitrary[String]) {
          (ips, otherConfig, destination, excluded) =>

            val ipString = ips.mkString(",")

            val config = Configuration(
              (otherConfig +
                ("bootstrap.filters.allowlist.destination" -> destination) +
                ("bootstrap.filters.allowlist.excluded"    -> excluded) +
                ("bootstrap.filters.allowlist.ips"         -> ipString) +
                ("bootstrap.filters.allowlist.enabled"     -> true) +
                healthCheckPath
              ).toSeq: _*
            )

            val allowlistFilter = new AllowlistFilter(config, mockMaterializer)

            allowlistFilter.allowlist should contain theSameElementsAs ips
        }
      }
    }
  }

  "the destination for non-allowlisted visitors" should {

    "throw an exception" when {

      "the underlying config value is not there" in {

        forAll(otherConfigGen, arbitrary[String], arbitrary[String]) {
          (otherConfig, destination, excluded) =>

            whenever(!otherConfig.contains("bootstrap.filters.allowlist.destination")) {

              val config = Configuration(
                (otherConfig +
                  ("bootstrap.filters.allowlist.ips"      -> destination) +
                  ("bootstrap.filters.allowlist.excluded" -> excluded) +
                  ("bootstrap.filters.allowlist.enabled"     -> true) +
                  healthCheckPath
                  ).toSeq: _*
              )

              assertThrows[ConfigException] {
                new AllowlistFilter(config, mockMaterializer).loadConfig
              }
            }
        }
      }
    }

    "return a Call to the destination" in {

      forAll(otherConfigGen, arbitrary[String], arbitrary[String], arbitrary[String]) {
        (otherConfig, ips, destination, excluded) =>

          val config = Configuration(
            (otherConfig +
              ("bootstrap.filters.allowlist.ips"         -> destination) +
              ("bootstrap.filters.allowlist.excluded"    -> excluded) +
              ("bootstrap.filters.allowlist.destination" -> destination) +
              ("bootstrap.filters.allowlist.enabled"     -> true) +
              healthCheckPath
              ).toSeq: _*
          )

          val allowlistFilter = new AllowlistFilter(config, mockMaterializer)

          allowlistFilter.destination shouldEqual Call("GET", destination)
      }
    }
  }

  "the list of excluded paths" should {

    "throw an exception" when {

      "the underlying config value is not there" in {

        forAll(otherConfigGen, arbitrary[String], arbitrary[String]) {
          (otherConfig, destination, excluded) =>

            whenever(!otherConfig.contains("bootstrap.filters.allowlist.excluded")) {

              val config = Configuration(
                (otherConfig +
                  ("bootstrap.filters.allowlist.destination" -> destination) +
                  ("bootstrap.filters.allowlist.ips"         -> excluded) +
                  ("bootstrap.filters.allowlist.enabled"     -> true) +
                  healthCheckPath
                  ).toSeq: _*
              )

              assertThrows[ConfigException] {
                new AllowlistFilter(config, mockMaterializer).loadConfig
              }
            }
        }
      }
    }

    "return Calls to all of the values" when {

      "given a comma-separated list of values" in {

        val gen = Gen.nonEmptyListOf(Gen.alphaNumStr suchThat (_.nonEmpty))

        forAll(gen, otherConfigGen, arbitrary[String], arbitrary[String]) {
          (excludedPaths, otherConfig, destination, ips) =>

            val excludedPathString = excludedPaths.mkString(",")

            val config = Configuration(
              (otherConfig +
                ("bootstrap.filters.allowlist.destination" -> destination) +
                ("bootstrap.filters.allowlist.excluded"    -> excludedPathString) +
                ("bootstrap.filters.allowlist.ips"         -> ips) +
                ("bootstrap.filters.allowlist.enabled"     -> true) +
                healthCheckPath
                ).toSeq: _*
            )

            val expectedCalls = excludedPaths.map(Call("GET", _))

            val allowlistFilter = new AllowlistFilter(config, mockMaterializer)

            allowlistFilter.excludedPaths should contain theSameElementsAs expectedCalls
        }
      }
    }

    "pass through requests" when {

      "the filter is disabled" in {

        val app = new GuiceApplicationBuilder()
          .configure(
            "bootstrap.filters.allowlist.destination" -> "",
            "bootstrap.filters.allowlist.excluded" -> "",
            "bootstrap.filters.allowlist.ips" -> "",
            "bootstrap.filters.allowlist.enabled" -> false
          )
          .build()

        val filter = app.injector.instanceOf[AllowlistFilter]

        val result = filter(_ => Future.successful(Results.Ok))(FakeRequest())

        status(result) shouldBe OK
      }
    }

    "not require the filter to be configured" when {

      "the filter is disabled" in {

        val app = new GuiceApplicationBuilder()
          .configure(
            "bootstrap.filters.allowlist.enabled" -> false
          )
          .build()

        val filter = app.injector.instanceOf[AllowlistFilter]

        val result = filter(_ => Future.successful(Results.Ok))(FakeRequest())

        status(result) shouldBe OK
      }
    }

    "filter requests" when {

      "the filter is enabled" in {

        val app = new GuiceApplicationBuilder()
          .configure(
            "bootstrap.filters.allowlist.destination" -> "",
            "bootstrap.filters.allowlist.excluded" -> "",
            "bootstrap.filters.allowlist.ips" -> "",
            "bootstrap.filters.allowlist.enabled" -> true,
              healthCheckPath
          )
          .build()

        val filter = app.injector.instanceOf[AllowlistFilter]

        val result = filter(_ => Future.successful(Results.Ok))(FakeRequest())

        status(result) shouldBe NOT_IMPLEMENTED
      }
    }

    "return successfully" when {
      "a valid `True-Client-IP` header is found" in {

        val app = new GuiceApplicationBuilder()
          .configure(
            "bootstrap.filters.allowlist.destination" -> govUkUrl,
            "bootstrap.filters.allowlist.excluded" -> "",
            "bootstrap.filters.allowlist.ips" -> allowedIpAddress,
            "bootstrap.filters.allowlist.enabled" -> true,
            healthCheckPath
          )
          .build()

        val filter = app.injector.instanceOf[AllowlistFilter]

        val result = filter(_ => Future.successful(Results.Ok))(
          FakeRequest()
            .withHeaders("True-Client-Ip" -> allowedIpAddress)
          )

        status(result) shouldBe OK

      }
    }
     "return a Redirect to the destination" when {
        "an invalid True-Client-IP header is found" in {

          val app = new GuiceApplicationBuilder()
            .configure(
              "bootstrap.filters.allowlist.destination" -> govUkUrl,
              "bootstrap.filters.allowlist.excluded" -> "",
              "bootstrap.filters.allowlist.ips" -> allowedIpAddress,
              "bootstrap.filters.allowlist.enabled" -> true,
              healthCheckPath
            )
            .build()

          val filter = app.injector.instanceOf[AllowlistFilter]

          val result = filter(_ => Future.successful(Results.Ok))(
            FakeRequest()
              .withHeaders("True-Client-Ip" -> nonAllowedIpAddress)
            )

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(govUkUrl)

        }
     }

    "return a Forbidden " when {
      "the user would endup in a redirect loop" in {

        val app = new GuiceApplicationBuilder()
          .configure(
            "bootstrap.filters.allowlist.destination" -> "/service-frontend",
            "bootstrap.filters.allowlist.excluded" -> "",
            "bootstrap.filters.allowlist.ips" -> allowedIpAddress,
            "bootstrap.filters.allowlist.enabled" -> true,
            healthCheckPath
          )
          .build()

        val filter = app.injector.instanceOf[AllowlistFilter]

        val result = filter(_ => Future.successful(Results.Ok))(
          FakeRequest("GET", "/service-frontend")
            .withHeaders("True-Client-Ip" -> nonAllowedIpAddress)
          )

        status(result) shouldBe FORBIDDEN

      }
    }


    "return OK " when {
      "the route to be accessed is an excluded path" in {

        val app = new GuiceApplicationBuilder()
          .configure(
            "bootstrap.filters.allowlist.destination" -> govUkUrl,
            "bootstrap.filters.allowlist.excluded" -> exclusion,
            "bootstrap.filters.allowlist.ips" -> allowedIpAddress,
            "bootstrap.filters.allowlist.enabled" -> true,
            healthCheckPath
          )
          .build()

        val filter = app.injector.instanceOf[AllowlistFilter]

        val result = filter(_ => Future.successful(Results.Ok))(
          FakeRequest("GET", exclusion)
            .withHeaders("True-Client-Ip" -> nonAllowedIpAddress)
          )

        status(result) shouldBe OK
      }
    }

    "return OK " when {
      "the route to be accessed is the healthcheck /ping/ping endpoint" in {

        val app = new GuiceApplicationBuilder()
          .configure(
            "bootstrap.filters.allowlist.destination" -> govUkUrl,
            "bootstrap.filters.allowlist.excluded" -> exclusion,
            "bootstrap.filters.allowlist.ips" -> allowedIpAddress,
            "bootstrap.filters.allowlist.enabled" -> true,
            healthCheckPath
          )
          .build()

        val filter = app.injector.instanceOf[AllowlistFilter]

        val result = filter(_ => Future.successful(Results.Ok))(
          FakeRequest("GET", "/ping/ping")
            .withHeaders("True-Client-Ip" -> nonAllowedIpAddress)
        )

        status(result) shouldBe OK
      }
    }

    "return OK " when {
      "the requested route matches a wildcard exclusion" in {

        val app = new GuiceApplicationBuilder()
          .configure(
            "bootstrap.filters.allowlist.destination" -> govUkUrl,
            "bootstrap.filters.allowlist.excluded" -> (exclusion + ",/service/feature/*"),
            "bootstrap.filters.allowlist.ips" -> allowedIpAddress,
            "bootstrap.filters.allowlist.enabled" -> true,
            healthCheckPath
          )
          .build()

        val filter = app.injector.instanceOf[AllowlistFilter]

        val result = filter(_ => Future.successful(Results.Ok))(
          FakeRequest("GET", "/service/feature/settings")
            .withHeaders("True-Client-Ip" -> nonAllowedIpAddress)
        )

        status(result) shouldBe OK
      }
    }

  }
}
