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
import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc.Results.{Forbidden, NotImplemented, Redirect}
import play.api.mvc.{Call, Filter, RequestHeader, Result}

import scala.concurrent.Future

@Singleton
class AllowlistFilter @Inject() (
  config: Configuration,
  override val mat: Materializer
) extends Filter {


  val trueClient = "True-Client-IP"

  case class AllowlistFilterConfig(
    allowlist    : Seq[String],
    destination  : Call,
    excludedPaths: Seq[Call]
  )

  private lazy val allowlistFilterConfig = AllowlistFilterConfig(
    allowlist =
      config.get[String]("bootstrap.filters.allowlist.ips")
        .split(",")
        .toIndexedSeq
        .map(_.trim)
        .filter(_.nonEmpty),

    destination = {
      val path = config.get[String]("bootstrap.filters.allowlist.destination")
      Call("GET", path)
    },

    excludedPaths =
      config.get[Seq[String]]("bootstrap.filters.allowlist.excluded")
        .toIndexedSeq
        .map(_.trim)
        .filter(_.nonEmpty)
        .map{_.split(":") match {
                case Array(method, url) => Call(method, url)
                case Array(url) => Call("GET", url)
              }
            }
    )

  def loadConfig: AllowlistFilter = {
    allowlistFilterConfig
    this
  }

  lazy val allowlist: Seq[String] =
    allowlistFilterConfig.allowlist

  @deprecated("Use allowlist instead", "4.0.0")
  def whitelist: Seq[String] =
    allowlist

  lazy val destination: Call =
    allowlistFilterConfig.destination

  lazy val excludedPaths: Seq[Call] =
    allowlistFilterConfig.excludedPaths

  private val enabled: Boolean =
    config.get[Boolean]("bootstrap.filters.allowlist.enabled")

 protected def noHeaderAction(f: RequestHeader => Future[Result], rh: RequestHeader): Future[Result] =
    Future.successful(NotImplemented)

  private def isCircularDestination(requestHeader: RequestHeader): Boolean =
    requestHeader.uri == destination.url

  protected def response: Result = Redirect(destination)

  protected def excluded(rh: RequestHeader): Boolean = {
    def wildcardMatch(c: Call) = c.url.endsWith("/*") && rh.uri.startsWith(c.url.dropRight(2))

    excludedPaths.exists(c => c.method == rh.method && (c.url == rh.uri || wildcardMatch(c)))
  }

  private def processRequest(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] =
    if (excluded(rh))
      f(rh)
    else
      rh.headers.get(trueClient).fold(noHeaderAction(f, rh))(ip =>
        if (allowlist.contains(ip))
          f(rh)
        else if (isCircularDestination(rh))
          Future.successful(Forbidden)
        else
          Future.successful(response)
      )


  override def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] =
    if (enabled)
      processRequest(f)(rh)
    else
      f(rh)
}
