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

package uk.gov.hmrc.play.bootstrap.binders

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.concurrent.ScalaFutures
import play.api.{Environment, Mode}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RedirectUrlBinderSpec extends AnyWordSpecLike with Matchers with ScalaFutures {

  "Should allow to get safe binding if all url's are permitted" in {
    val policy = UnsafePermitAll

    new RedirectUrl("http://www.google.com").get(policy)       shouldBe SafeRedirectUrl("http://www.google.com")
    new RedirectUrl("http://www.google.com").getEither(policy) shouldBe Right(SafeRedirectUrl("http://www.google.com"))
    new RedirectUrl("http://www.google.com").unsafeValue       shouldBe "http://www.google.com"

    new RedirectUrl("/test").get(policy)       shouldBe SafeRedirectUrl("/test")
    new RedirectUrl("/test").getEither(policy) shouldBe Right(SafeRedirectUrl("/test"))
    new RedirectUrl("/test").unsafeValue       shouldBe "/test"
  }

  "Should allow to match if only relative url's are supported" in {
    val policy = OnlyRelative

    new RedirectUrl("/test").getEither(policy) shouldBe Right(SafeRedirectUrl("/test"))
    new RedirectUrl("http://www.google.com").getEither(policy) shouldBe Left(
      "Provided URL [http://www.google.com] doesn't comply with redirect policy")
  }

  "Should allow to match absolute url's with hostnames from specified allowlist" in {
    val policy = AbsoluteWithHostnameFromAllowlist("www.test1.com")

    new RedirectUrl("http://www.test1.com/foo/bar").getEither(policy) shouldBe Right(
      SafeRedirectUrl("http://www.test1.com/foo/bar"))
    new RedirectUrl("http://www.test1.com").getEither(policy) shouldBe Right(SafeRedirectUrl("http://www.test1.com"))
    new RedirectUrl("http://www.test2.com").getEither(policy) shouldBe Left(
      "Provided URL [http://www.test2.com] doesn't comply with redirect policy")
    new RedirectUrl("/test").getEither(policy) shouldBe Left("Provided URL [/test] doesn't comply with redirect policy")
  }

  "Should allow to match all url's if run mode is Dev" in {
    val policyDev = PermitAllOnDev(Environment.simple(mode = Mode.Dev))

    new RedirectUrl("http://www.google.com").get(policyDev) shouldBe SafeRedirectUrl("http://www.google.com")
    new RedirectUrl("/test").get(policyDev) shouldBe SafeRedirectUrl("/test")
    new RedirectUrl("http://www.google.com").getEither(policyDev) shouldBe Right(SafeRedirectUrl("http://www.google.com"))
    new RedirectUrl("/test").getEither(policyDev) shouldBe Right(SafeRedirectUrl("/test"))

    val policyTest = PermitAllOnDev(Environment.simple(mode = Mode.Test))

    new RedirectUrl("http://www.google.com").getEither(policyTest) shouldBe Left("Provided URL [http://www.google.com] doesn't comply with redirect policy")
    new RedirectUrl("/test").getEither(policyTest) shouldBe Left("Provided URL [/test] doesn't comply with redirect policy")
  }

  "It should be possible to combine multiple policies" in {
    val policy = OnlyRelative | AbsoluteWithHostnameFromAllowlist(Set("www.test1.com"))

    new RedirectUrl("http://www.test1.com/foo/bar").getEither(policy) shouldBe Right(
      SafeRedirectUrl("http://www.test1.com/foo/bar"))
    new RedirectUrl("http://www.test1.com").getEither(policy) shouldBe Right(SafeRedirectUrl("http://www.test1.com"))
    new RedirectUrl("http://www.test2.com").getEither(policy) shouldBe Left(
      "Provided URL [http://www.test2.com] doesn't comply with redirect policy")
    new RedirectUrl("/test").getEither(policy) shouldBe Right(SafeRedirectUrl("/test"))
  }

  "It should be possible to use policies fetched from futures" in {

    def receiveAllowlist(): Future[Set[String]] = Future.successful(Set("www.test1.com"))

    val policy = OnlyRelative | AbsoluteWithHostnameFromAllowlist(receiveAllowlist())

    new RedirectUrl("http://www.test1.com/foo/bar").getEither(policy).futureValue shouldBe Right(
      SafeRedirectUrl("http://www.test1.com/foo/bar"))
    new RedirectUrl("http://www.test1.com").getEither(policy).futureValue shouldBe Right(
      SafeRedirectUrl("http://www.test1.com"))
    new RedirectUrl("http://www.test2.com").getEither(policy).futureValue shouldBe Left(
      "Provided URL [http://www.test2.com] doesn't comply with redirect policy")
    new RedirectUrl("/test").getEither(policy).futureValue shouldBe Right(SafeRedirectUrl("/test"))
  }
}
