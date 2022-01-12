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
import play.api.mvc.Call

class AllowlistFilterSpec
  extends AnyWordSpecLike
     with Matchers
     with ScalaCheckDrivenPropertyChecks
     with MockitoSugar {

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
                  ("bootstrap.filters.allowlist.excluded"    -> excluded)
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
                ("bootstrap.filters.allowlist.ips"         -> "")
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
                ("bootstrap.filters.allowlist.ips"         -> ipString)
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
                  ("bootstrap.filters.allowlist.excluded" -> excluded)
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
              ("bootstrap.filters.allowlist.destination" -> destination)
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
                  ("bootstrap.filters.allowlist.ips"         -> excluded)
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
                ("bootstrap.filters.allowlist.ips"         -> ips)
                ).toSeq: _*
            )

            val expectedCalls = excludedPaths.map(Call("GET", _))

            val allowlistFilter = new AllowlistFilter(config, mockMaterializer)

            allowlistFilter.excludedPaths should contain theSameElementsAs expectedCalls
        }
      }
    }
  }
}
