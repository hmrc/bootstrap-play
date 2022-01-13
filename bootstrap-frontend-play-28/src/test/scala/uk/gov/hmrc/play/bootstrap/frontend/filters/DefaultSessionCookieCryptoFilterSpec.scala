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

import javax.inject.Inject
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{LoneElement, OptionValues}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.{DefaultHttpFilters, HttpFilters}
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.api.routing.Router
import play.api.test.Helpers._
import uk.gov.hmrc.crypto._
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto._
import scala.reflect.ClassTag

class DefaultSessionCookieCryptoFilterSpec
    extends AnyWordSpec
    with GuiceOneServerPerSuite
    with Matchers
    with OptionValues
    with MockitoSugar
    with ScalaFutures
    with IntegrationPatience
    with LoneElement {

  import DefaultSessionCookieCryptoFilterSpec.Filters

  val existingSessionData = Map("foo" -> "bar")
  val newSessionData      = Map("baz" -> "quux")

  "Filter" should {
    "decrypt session cookie from request, invoke next action and encrypt session cookie" in {
      val httpClient           = instanceOf[WSClient]
      val sessionBaker         = instanceOf[SessionCookieBaker]
      val cookieHeaderEncoding = instanceOf[CookieHeaderEncoding]
      val sessionCookieCrypto  = instanceOf[SessionCookieCrypto]

      val sessionCookie               = sessionBaker.encodeAsCookie(Session(existingSessionData))
      val encryptedSessionCookieValue = sessionCookieCrypto.crypto.encrypt(PlainText(sessionCookie.value)).value
      val encryptedSessionCookie      = sessionCookie.copy(value = encryptedSessionCookieValue)
      val cookieAsString              = cookieHeaderEncoding.encodeCookieHeader(Seq(encryptedSessionCookie))

      val result = httpClient
        .url(s"http://localhost:$port")
        .withHttpHeaders(COOKIE -> cookieAsString)
        .get()
        .futureValue

      val setCookieHeader      = result.header(SET_COOKIE).get
      val decodedSessionCookie = cookieHeaderEncoding.decodeSetCookieHeader(setCookieHeader).loneElement
      val decryptedSession     = sessionCookieCrypto.crypto.decrypt(Crypted(decodedSessionCookie.value)).value
      val session              = sessionBaker.decode(decryptedSession)

      session shouldBe existingSessionData ++ newSessionData
    }
  }

  override def fakeApplication(): Application = {
    import play.api.routing.sird._
    val Action = stubControllerComponents().actionBuilder

    new GuiceApplicationBuilder()
      .configure("cookie.encryption.key" -> "gvBoGdgzqG1AarzF1LY0zQ==")
      .router(Router.from {
        case GET(p"/") =>
          Action { implicit request =>
            request.session.data shouldBe existingSessionData
            Results.Ok.addingToSession(newSessionData.toSeq: _*)
        }
      })
      .overrides(
        bind[HttpFilters].to[Filters],
        bind[ApplicationCrypto].toProvider[ApplicationCryptoProvider],
        bind[SessionCookieCrypto].toProvider[SessionCookieCryptoProvider],
        bind[SessionCookieCryptoFilter].to[DefaultSessionCookieCryptoFilter]
      )
      .disable(classOf[CookiesModule])
      .bindings(new LegacyCookiesModule)
      .build()
  }

  private def instanceOf[T](implicit ct: ClassTag[T]): T =
    app.injector.instanceOf(ct.runtimeClass.asInstanceOf[Class[T]])
}

object DefaultSessionCookieCryptoFilterSpec {

  class Filters @Inject()(cryptoFilter: SessionCookieCryptoFilter) extends DefaultHttpFilters(cryptoFilter)
}
