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

import java.net.{URL, URLEncoder}

import play.api.{Environment, Mode}
import play.api.mvc.QueryStringBindable
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrlPolicy.Id

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object RedirectUrlPolicy {
  type Id[A] = A

  implicit def toFuture(in: RedirectUrlPolicy[Id]): RedirectUrlPolicy[Future] = new RedirectUrlPolicy[Future] {
    override def applies(url: String): Future[Boolean] = Future.successful(in.applies(url))
  }
}

case class SafeRedirectUrl(url: String) extends AnyVal {
  override def toString = url
  def encodedUrl        = URLEncoder.encode(url, "UTF-8")
}

sealed trait RedirectUrlPolicy[T[_]] {
  self =>
  def applies(url: String): T[Boolean]
  def |(that: RedirectUrlPolicy[T])(implicit f: Applicative[T]): RedirectUrlPolicy[T] = new RedirectUrlPolicy[T] {
    override def applies(url: String): T[Boolean] =
      f.map(f.product(self.applies(url), that.applies(url)))(results => results._1 || results._2)
  }
}

case object UnsafePermitAll extends RedirectUrlPolicy[Id] {
  override def applies(url: String): Id[Boolean] = true
}

case object OnlyRelative extends RedirectUrlPolicy[Id] {
  override def applies(url: String): Boolean = RedirectUrl.isRelativeUrl(url)
}

case class PermitAllOnDev(environment: Environment) extends RedirectUrlPolicy[Id] {
  override def applies(url: String): Id[Boolean] = environment.mode == Mode.Dev
}

object AbsoluteWithHostnameFromWhitelist {

  def apply(allowedHosts: String*) = new RedirectUrlPolicy[Id] {
    override def applies(url: String): Id[Boolean] = Try(new URL(url)) match {
      case Success(parsedUrl) if allowedHosts.contains(parsedUrl.getHost) => true
      case _                                                              => false
    }
  }

  def apply(allowedHosts: Set[String]) = new RedirectUrlPolicy[Id] {
    override def applies(url: String): Id[Boolean] = Try(new URL(url)) match {
      case Success(parsedUrl) if allowedHosts.contains(parsedUrl.getHost) => true
      case _                                                              => false
    }
  }

  def apply(allowedHostsFn: => Future[Set[String]])(implicit ec: ExecutionContext) = new RedirectUrlPolicy[Future] {
    override def applies(url: String): Future[Boolean] = Try(new URL(url)) match {
      case Success(parsedUrl) => for (allowedHosts <- allowedHostsFn) yield allowedHosts.contains(parsedUrl.getHost)
      case _                  => Future.successful(false)
    }
  }
}

@scala.annotation.implicitNotFound(
  "You have to add the following import: 'import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl._' ")
trait Applicative[T[_]] {
  def map[K, V](t: T[K])(fn: K => V): T[V]
  def product[A, B](fa: T[A], fb: T[B]): T[(A, B)]
}

case class RedirectUrl(private val url: String) {

  require(
    (RedirectUrl.isRelativeUrl(url) || RedirectUrl.isAbsoluteUrl(url)) && !url.contains("@"),
    RedirectUrl.errorFor(url)
  )

  def getEither[T[_]](policy: RedirectUrlPolicy[T])(implicit f: Applicative[T]) =
    f.map(policy.applies(url)) { result =>
      if (result) {
        Right(SafeRedirectUrl(url))
      } else {
        Left(s"Provided URL [$url] doesn't comply with redirect policy")
      }
    }

  def get[T[_]](policy: RedirectUrlPolicy[T])(implicit f: Applicative[T]) =
    f.map(getEither(policy))(
      _.fold[SafeRedirectUrl](
        message => throw new IllegalArgumentException(message),
        value => value
      ))

  val unsafeValue = url
}

object RedirectUrl {

  private def errorFor(invalidUrl: String) = s"'$invalidUrl' is not a valid continue URL"

  def isAbsoluteUrl(url: String) = url.startsWith("http")

  def isRelativeUrl(url: String) = url.matches("""^[/][^/\\].*""")

  implicit def queryBinder(implicit stringBinder: QueryStringBindable[String]) = new QueryStringBindable[RedirectUrl] {
    def bind(key: String, params: Map[String, Seq[String]]) =
      stringBinder.bind(key, params).map {
        case Right(s) =>
          Try(RedirectUrl(s)) match {
            case Success(url) => Right(url)
            case Failure(_)   => Left(errorFor(s))
          }
        case Left(message) => Left(message)
      }

    def unbind(key: String, value: RedirectUrl) = stringBinder.unbind(key, value.url)

  }

  implicit def idFunctor = new Applicative[Id] {
    override def map[K, V](t: Id[K])(fn: K => V): Id[V] = fn(t)

    override def product[A, B](fa: Id[A], fb: Id[B]): (A, B) = (fa, fb)
  }

  implicit def futureFunctor(implicit ec: ExecutionContext) = new Applicative[Future] {
    override def map[K, V](t: Future[K])(fn: K => V): Future[V] = t.map(fn)

    override def product[A, B](fa: Future[A], fb: Future[B]): Future[(A, B)] =
      for {
        a <- fa
        b <- fb
      } yield (a, b)
  }
}
