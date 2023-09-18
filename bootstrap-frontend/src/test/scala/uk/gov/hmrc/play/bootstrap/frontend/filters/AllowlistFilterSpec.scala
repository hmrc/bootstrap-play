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
import play.api.{Configuration, PlayException}
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

  val exclusions = Seq("/ping/ping", "/some-excluded-path")

  val nonAllowedIpAddress = "10.0.0.1"
  val govUkUrl            = "https://www.gov.uk"
  val allowedIpAddress    = "192.168.2.1"
  val mockMaterializer    = mock[Materializer]

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
                (otherConfig ++
                  Map(
                    "bootstrap.filters.allowlist.destination" -> destination,
                    "bootstrap.filters.allowlist.excluded"    -> excluded,
                    "bootstrap.filters.allowlist.enabled"     -> true
                  )
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
        forAll(otherConfigGen, arbitrary[String], arbitrary[Seq[String]]) {
          (otherConfig, redirectUrlWhenDenied, excluded) =>

            val config = Configuration(
              (otherConfig ++
                Map(
                  "bootstrap.filters.allowlist.redirectUrlWhenDenied" -> redirectUrlWhenDenied,
                  "bootstrap.filters.allowlist.excluded"              -> excluded,
                  "bootstrap.filters.allowlist.ips"                   -> Seq.empty,
                  "bootstrap.filters.allowlist.enabled"               -> true
                )
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

        forAll(gen, otherConfigGen, arbitrary[String], arbitrary[Seq[String]]) {
          (ips, otherConfig, redirectUrlWhenDenied, excluded) =>

            val config = Configuration(
              (otherConfig ++
                Map(
                  "bootstrap.filters.allowlist.redirectUrlWhenDenied" -> redirectUrlWhenDenied,
                  "bootstrap.filters.allowlist.excluded"              -> excluded,
                  "bootstrap.filters.allowlist.ips"                   -> ips,
                  "bootstrap.filters.allowlist.enabled"               -> true
                )
              ).toSeq: _*
            )

            val allowlistFilter = new AllowlistFilter(config, mockMaterializer)

            allowlistFilter.allowlist should contain theSameElementsAs ips
        }
      }
    }
  }

  "the bootstrap.filters.allowlist.destination key is deprecated and" should {
    "throw an exception" when {
      "a value exists" in {
        val gen = Gen.nonEmptyListOf(Gen.alphaNumStr suchThat (_.nonEmpty))

        forAll(gen, otherConfigGen, arbitrary[Seq[String]], arbitrary[Seq[String]]) {
          (destination, otherConfig, ips, excluded) =>

            whenever(!otherConfig.contains("bootstrap.filters.allowlist.redirectUrlWhenDenied")) {
              val config = Configuration(
                (otherConfig ++
                  Map(
                    "bootstrap.filters.allowlist.ips"         -> ips,
                    "bootstrap.filters.allowlist.destination" -> destination,
                    "bootstrap.filters.allowlist.excluded"    -> excluded,
                    "bootstrap.filters.allowlist.enabled"     -> true
                  )
                ).toSeq: _*
              )

              assertThrows[PlayException] {
                new AllowlistFilter(config, mockMaterializer).loadConfig
              }
            }
        }
      }
    }
  }

  "the redirectUrlWhenDenied for non-allowlisted visitors" should {
    "throw an exception" when {
      "the underlying config value is not there" in {
        forAll(otherConfigGen, arbitrary[String], arbitrary[String]) {
          (otherConfig, ips, excluded) =>

            whenever(!otherConfig.contains("bootstrap.filters.allowlist.redirectUrlWhenDenied")) {
              val config = Configuration(
                (otherConfig ++
                  Map(
                    "bootstrap.filters.allowlist.ips"      -> ips,
                    "bootstrap.filters.allowlist.excluded" -> excluded,
                    "bootstrap.filters.allowlist.enabled"  -> true
                  )
                ).toSeq: _*
              )

              assertThrows[ConfigException] {
                new AllowlistFilter(config, mockMaterializer).loadConfig
              }
            }
        }
      }
    }

    "return a Call to the redirectUrlWhenDenied" in {
      forAll(otherConfigGen, arbitrary[Seq[String]], arbitrary[String], arbitrary[Seq[String]]) {
        (otherConfig, ips, redirectUrlWhenDenied, excluded) =>

          val config = Configuration(
            (otherConfig ++
              Map(
                "bootstrap.filters.allowlist.ips"                   -> ips,
                "bootstrap.filters.allowlist.excluded"              -> excluded,
                "bootstrap.filters.allowlist.redirectUrlWhenDenied" -> redirectUrlWhenDenied,
                "bootstrap.filters.allowlist.enabled"               -> true
              )
            ).toSeq: _*
          )

          val allowlistFilter = new AllowlistFilter(config, mockMaterializer)

          allowlistFilter.redirectUrlWhenDenied shouldEqual Call("GET", redirectUrlWhenDenied)
      }
    }
  }

  "the list of excluded paths" should {
    "throw an exception" when {
      "the underlying config value is not there" in {
        forAll(otherConfigGen, arbitrary[String], arbitrary[String]) {
          (otherConfig, redirectUrlWhenDenied, ips) =>

            whenever(!otherConfig.contains("bootstrap.filters.allowlist.excluded")) {

              val config = Configuration(
                (otherConfig ++
                  Map(
                    "bootstrap.filters.allowlist.redirectUrlWhenDenied" -> redirectUrlWhenDenied,
                    "bootstrap.filters.allowlist.ips"                   -> ips,
                    "bootstrap.filters.allowlist.enabled"               -> true
                  )
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
      "given an array of excluded paths" in {
        val gen = Gen.nonEmptyListOf(Gen.alphaNumStr suchThat (_.nonEmpty))

        forAll(gen, otherConfigGen, arbitrary[String], arbitrary[Seq[String]]) {
          (excludedPaths, otherConfig, redirectUrlWhenDenied, ips) =>

            val config = Configuration(
              (otherConfig ++
                Map(
                  "bootstrap.filters.allowlist.redirectUrlWhenDenied" -> redirectUrlWhenDenied,
                  "bootstrap.filters.allowlist.excluded"              -> excludedPaths,
                  "bootstrap.filters.allowlist.ips"                   -> ips,
                  "bootstrap.filters.allowlist.enabled"               -> true
                )
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
            "bootstrap.filters.allowlist.redirectUrlWhenDenied" -> "",
            "bootstrap.filters.allowlist.excluded"              -> "",
            "bootstrap.filters.allowlist.ips"                   -> "",
            "bootstrap.filters.allowlist.enabled"               -> false
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
            "bootstrap.filters.allowlist.redirectUrlWhenDenied" -> "",
            "bootstrap.filters.allowlist.excluded"              -> Seq.empty,
            "bootstrap.filters.allowlist.ips"                   -> Seq.empty,
            "bootstrap.filters.allowlist.enabled"               -> true
          )
          .build()

        val filter = app.injector.instanceOf[AllowlistFilter]

        val result = filter(_ => Future.successful(Results.Ok))(FakeRequest())

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "return successfully" when {
      "a valid `True-Client-IP` header is found" in {
        val app = new GuiceApplicationBuilder()
          .configure(
            "bootstrap.filters.allowlist.redirectUrlWhenDenied" -> govUkUrl,
            "bootstrap.filters.allowlist.excluded"              -> Seq.empty,
            "bootstrap.filters.allowlist.ips"                   -> Seq(allowedIpAddress),
            "bootstrap.filters.allowlist.enabled"               -> true
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
     "return a Redirect to the 'redirectUrlWhenDenied'" when {
        "an invalid True-Client-IP header is found" in {
          val app = new GuiceApplicationBuilder()
            .configure(
              "bootstrap.filters.allowlist.redirectUrlWhenDenied" -> govUkUrl,
              "bootstrap.filters.allowlist.excluded"              -> Seq.empty,
              "bootstrap.filters.allowlist.ips"                   -> Seq(allowedIpAddress),
              "bootstrap.filters.allowlist.enabled"               -> true
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

    "return a Forbidden" when {
      "the user would endup in a redirect loop" in {
        val app = new GuiceApplicationBuilder()
          .configure(
            "bootstrap.filters.allowlist.redirectUrlWhenDenied" -> "/service-frontend",
            "bootstrap.filters.allowlist.excluded"              -> Seq.empty,
            "bootstrap.filters.allowlist.ips"                   -> Seq(allowedIpAddress),
            "bootstrap.filters.allowlist.enabled"               -> true
          )
          .build()

        val filter = app.injector.instanceOf[AllowlistFilter]

        val result = filter(_ => Future.successful(Results.Ok))(
          FakeRequest("GET", "/service-frontend")
            .withHeaders("True-Client-Ip" -> nonAllowedIpAddress)
          )

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "return OK" when {
      "the route to be accessed is an excluded path" in {
        val app = new GuiceApplicationBuilder()
          .configure(
            "bootstrap.filters.allowlist.redirectUrlWhenDenied" -> govUkUrl,
            "bootstrap.filters.allowlist.excluded"              -> exclusions,
            "bootstrap.filters.allowlist.ips"                   -> Seq(allowedIpAddress),
            "bootstrap.filters.allowlist.enabled"               -> true
          )
          .build()

        val filter = app.injector.instanceOf[AllowlistFilter]

        val result = filter(_ => Future.successful(Results.Ok))(
          FakeRequest("GET", "/some-excluded-path")
            .withHeaders("True-Client-Ip" -> nonAllowedIpAddress)
          )

        status(result) shouldBe OK
      }
    }

    "return OK" when {
      "the route to be accessed is an excluded route accessed with an http method other than GET" in {
        val app = new GuiceApplicationBuilder()
          .configure(
            "bootstrap.filters.allowlist.redirectUrlWhenDenied" -> govUkUrl,
            "bootstrap.filters.allowlist.excluded"              -> Seq("/ping/ping", "PUT:/some-excluded-path"),
            "bootstrap.filters.allowlist.ips"                   -> Seq(allowedIpAddress),
            "bootstrap.filters.allowlist.enabled"               -> true
          )
          .build()

        val filter = app.injector.instanceOf[AllowlistFilter]

        val result = filter(_ => Future.successful(Results.Ok))(
          FakeRequest("PUT", "/some-excluded-path")
            .withHeaders("True-Client-Ip" -> nonAllowedIpAddress)
        )

        status(result) shouldBe OK
      }
    }

    "be tolerant" when {
      val app = new GuiceApplicationBuilder()
        .configure(
          "bootstrap.filters.allowlist.redirectUrlWhenDenied" -> govUkUrl,
          "bootstrap.filters.allowlist.excluded"              -> Seq("/ping/ping", "put:/some-excluded-path"),
          "bootstrap.filters.allowlist.ips"                   -> Seq(allowedIpAddress),
          "bootstrap.filters.allowlist.enabled"               -> true
        )
        .build()

      val filter = app.injector.instanceOf[AllowlistFilter]

      "configuration for exclusions has the method defined as non-uppercase" in {
        val result = filter(_ => Future.successful(Results.Ok))(
          FakeRequest("PUT", "/some-excluded-path")
            .withHeaders("True-Client-Ip" -> nonAllowedIpAddress)
        )
        status(result) shouldBe OK
      }
      "the request method is non-uppercase" in {
        val result = filter(_ => Future.successful(Results.Ok))(
          FakeRequest("Put", "/some-excluded-path")
            .withHeaders("True-Client-Ip" -> nonAllowedIpAddress)
        )
        status(result) shouldBe OK
      }
    }

    "return OK " when {
      "the route to be accessed is the healthcheck /ping/ping endpoint" in {
        val app = new GuiceApplicationBuilder()
          .configure(
            "bootstrap.filters.allowlist.redirectUrlWhenDenied" -> govUkUrl,
            "bootstrap.filters.allowlist.excluded"              -> exclusions,
            "bootstrap.filters.allowlist.ips"                   -> Seq(allowedIpAddress),
            "bootstrap.filters.allowlist.enabled"               -> true
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
      "the requested route matches a wildcarded exclusion" in {
        val app = new GuiceApplicationBuilder()
          .configure(
            "bootstrap.filters.allowlist.redirectUrlWhenDenied" -> govUkUrl,
            "bootstrap.filters.allowlist.excluded"              -> (exclusions :+ "/service/feature/*"),
            "bootstrap.filters.allowlist.ips"                   -> Seq(allowedIpAddress),
            "bootstrap.filters.allowlist.enabled"               -> true
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
