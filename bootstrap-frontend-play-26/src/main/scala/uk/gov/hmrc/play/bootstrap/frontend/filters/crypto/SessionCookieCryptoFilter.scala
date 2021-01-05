/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.play.bootstrap.frontend.filters.crypto

import akka.stream.Materializer
import javax.inject.Inject
import play.api.Logger
import play.api.http.HeaderNames
import play.api.mvc._
import play.api.mvc.request.{Cell, RequestAttrKey}
import uk.gov.hmrc.crypto._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

trait CryptoImplicits {

  protected implicit def strToPlain(s: String): PlainContent =
    PlainText(s)

  protected implicit def strToCrypt(s: String): Crypted =
    Crypted(s)

  protected implicit def plainToString(p: PlainText): String =
    p.value

  protected implicit def cryptToString(c: Crypted): String =
    c.value
}

trait SessionCookieCryptoFilter extends Filter with CryptoImplicits {

  protected implicit def ec: ExecutionContext

  protected def encrypter: Encrypter
  protected def decrypter: Decrypter
  protected def sessionBaker: SessionCookieBaker
  protected val decodeCookieHeader: String => Seq[Cookie] = Cookies.decodeCookieHeader

  private val logger = Logger(getClass)

  override def apply(next: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] =
    encryptSession(next(decryptSession(rh)))

  private def encryptSession(f: Future[Result]): Future[Result] = f.map { result =>
    val newSessionAsCookie: Option[Cookie] =
      result.newSession.map { session =>
        val sessionCookie = sessionBaker.encodeAsCookie(session)
        sessionCookie.copy(value = encrypter.encrypt(sessionCookie.value))
      }

    // done to prevent discarding session cookie in SET_COOKIE header
    // as would be done in play.api.mvc.Result#bakeCookies
    val resultWithSessionReplacedByCookie: Option[Result] =
      newSessionAsCookie
        .map { c =>
          result
            .copy(newSession = None)
            .withCookies(c)
        }

    resultWithSessionReplacedByCookie.getOrElse(result)
  }

  private def decryptSession(rh: RequestHeader) =
    (for {
      encryptedSessionCookie <- findSessionCookie(rh)
      decryptedSessionCookie <- decrypt(encryptedSessionCookie)
    } yield {
      rh.addAttr(
        key   = RequestAttrKey.Session,
        value = Cell(sessionBaker.decodeFromCookie(Some(decryptedSessionCookie)))
      )
    }).getOrElse(rh)

  private def findSessionCookie(rh: RequestHeader): Option[Cookie] =
    rh.headers
      .getAll(HeaderNames.COOKIE)
      .flatMap(decodeCookieHeader)
      .find(_.name == sessionBaker.COOKIE_NAME)

  private def decrypt(cookie: Cookie): Option[Cookie] =
    Try(decrypter.decrypt(cookie.value)) match {
      case Success(decryptedValue) => Some(cookie.copy(value = decryptedValue))
      case Failure(ex) =>
        logger.warn(s"Could not decrypt cookie ${sessionBaker.COOKIE_NAME} got exception:${ex.getMessage}")
        None
    }
}

class DefaultSessionCookieCryptoFilter @Inject()(
  sessionCookieCrypto: SessionCookieCrypto,
  val sessionBaker: SessionCookieBaker
)(implicit
  override val mat: Materializer,
  override val ec: ExecutionContext
) extends SessionCookieCryptoFilter {
  override protected lazy val encrypter: Encrypter = sessionCookieCrypto.crypto
  override protected lazy val decrypter: Decrypter = sessionCookieCrypto.crypto
}
