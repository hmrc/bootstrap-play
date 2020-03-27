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

package uk.gov.hmrc.play.bootstrap.filters.frontend

import akka.stream.Materializer
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{LoneElement, Matchers, WordSpec}
import play.api.http.HeaderNames
import play.api.mvc.Results._
import play.api.mvc.{Cookie, Session, SessionCookieBaker}
import play.api.test.FakeRequest
import uk.gov.hmrc.crypto.{Crypted, Decrypter, Encrypter, PlainText}
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoFilter

import concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class SessionCookieCryptoFilterSpec
    extends WordSpec
    with Matchers
    with MockitoSugar
    with ScalaFutures
    with LoneElement {

  "Filter" should {

    "decrypt session cookie and make values available as session on a request" in new Setup {
      val allCookies = "all cookies encoded as one string"
      val request    = FakeRequest().withHeaders(HeaderNames.COOKIE -> allCookies)

      val encryptedSessionCookie = Cookie(cookieName, "encrypted session cookie value")
      when(mockedCookieDecoder(allCookies)).thenReturn(Seq(encryptedSessionCookie))

      val decryptedSessionCookieValue = "decrypted session cookie value"
      when(mockedDecrypter.decrypt(Crypted(encryptedSessionCookie.value)))
        .thenReturn(PlainText(decryptedSessionCookieValue))

      val decodedSession = Session(Map("foo" -> "bar"))
      when(mockedSessionBaker.decodeFromCookie(Some(Cookie(cookieName, decryptedSessionCookieValue))))
        .thenReturn(decodedSession)

      filter.apply { request =>
        request.session shouldBe decodedSession
        Future(Ok)
      }(request)

    }

    "not create new cookies if nothing was added to session on a result" in new Setup {
      val result = filter.apply(_ => Future(Ok))(FakeRequest()).futureValue

      result.newSession shouldBe None
      result.newCookies shouldBe Nil

      verifyZeroInteractions(mockedSessionBaker, mockedEncrypter)
    }

    "ignore non-session cookies on incoming requests" in new Setup {
      val allCookies         = "all cookies encoded as one string"
      val requestWithCookies = FakeRequest().withHeaders(HeaderNames.COOKIE -> allCookies)

      val someNonSessionCookies = Seq(Cookie("some-cookie-name", "some-value"))
      when(mockedCookieDecoder(allCookies)).thenReturn(someNonSessionCookies)

      filter.apply { request =>
        request shouldBe requestWithCookies
        Future(Ok)
      }(requestWithCookies)

      verifyZeroInteractions(mockedDecrypter, mockedEncrypter)
      verify(mockedSessionBaker).COOKIE_NAME
      verifyNoMoreInteractions(mockedSessionBaker)

    }

    "not encrypt new non-session cookies on a result" in new Setup {
      val cookie = Cookie("not-a-session", "value")
      val result =
        filter
          .apply(_ => Future(Ok.withCookies(cookie)))(FakeRequest())
          .futureValue

      result.newSession shouldBe None
      result.newCookies shouldBe Seq(cookie)

      verifyZeroInteractions(mockedCookieDecoder, mockedEncrypter, mockedDecrypter, mockedSessionBaker)
    }

    "set SET_COOKIE header with encrypted session cookie when session was modified" in new Setup {
      val request        = FakeRequest()
      val returnedResult = Ok.addingToSession("foo" -> "bar")(request)

      val encodedSessionCookieValue = "encoded session cookie value"
      when(mockedSessionBaker.encodeAsCookie(returnedResult.newSession.get))
        .thenReturn(Cookie(cookieName, encodedSessionCookieValue))

      val encryptedCookieValue = "encrypted value"
      when(mockedEncrypter.encrypt(PlainText(encodedSessionCookieValue)))
        .thenReturn(Crypted(encryptedCookieValue))

      val result =
        filter
          .apply(_ => Future(returnedResult))(request)
          .futureValue

      result.newSession             shouldBe None
      result.newCookies.loneElement shouldBe Cookie(cookieName, encryptedCookieValue)

    }

    "not change the request if session cookie was not available on it" in new Setup {
      val incomingRequest = FakeRequest()

      filter.apply { request =>
        request shouldBe incomingRequest
        Future(Ok)
      }(incomingRequest)

      verifyZeroInteractions(mockedDecrypter, mockedEncrypter, mockedSessionBaker)
    }

  }

  private trait Setup {
    val mockedEncrypter: Encrypter                 = mock[Encrypter]
    val mockedDecrypter: Decrypter                 = mock[Decrypter]
    val mockedSessionBaker: SessionCookieBaker     = mock[SessionCookieBaker]
    val mockedCookieDecoder: String => Seq[Cookie] = mock[String => Seq[Cookie]]

    val filter: SessionCookieCryptoFilter = new SessionCookieCryptoFilter {
      protected implicit val ec: ExecutionContext    = global
      protected val encrypter: Encrypter             = mockedEncrypter
      protected val decrypter: Decrypter             = mockedDecrypter
      protected val sessionBaker: SessionCookieBaker = mockedSessionBaker
      implicit def mat: Materializer                 = ???
      override protected val decodeCookieHeader      = mockedCookieDecoder
    }

    val cookieName = "n/a"
    when(mockedSessionBaker.COOKIE_NAME).thenReturn(cookieName)

  }

}
