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

import uk.gov.hmrc.play.bootstrap.stream.Materializer
import com.google.inject.{Inject, Singleton}
import play.api.{Configuration, Logger}
import play.api.mvc.Results.{InternalServerError, Redirect}
import play.api.mvc.{Call, Filter, RequestHeader, Result}

import scala.concurrent.Future

@Singleton
class AllowlistFilter @Inject() (
  config: Configuration,
  override val mat: Materializer
) extends Filter {

  private val logger = Logger(getClass)

  private val trueClient = "True-Client-IP"

  case class AllowlistFilterConfig(
    allowlist            : Seq[String],
    redirectUrlWhenDenied: Call,
    excludedPaths        : Seq[Call]
  )

  private val destinationKey           = "bootstrap.filters.allowlist.destination"
  private val redirectUrlWhenDeniedKey = "bootstrap.filters.allowlist.redirectUrlWhenDenied"

  private lazy val allowlistFilterConfig = AllowlistFilterConfig(
    allowlist             = config.get[Seq[String]]("bootstrap.filters.allowlist.ips")
                              .toIndexedSeq
                              .map(_.trim)
                              .filter(_.nonEmpty),

    redirectUrlWhenDenied = if (config.has(destinationKey))
                             throw config.reportError(destinationKey, s"$destinationKey is obsolete please use $redirectUrlWhenDeniedKey instead")
                            else {
                              val path = config.get[String](redirectUrlWhenDeniedKey)
                              Call("GET", path)
                            },

    excludedPaths         = config.get[Seq[String]]("bootstrap.filters.allowlist.excluded")
                              .toIndexedSeq
                              .map(_.trim)
                              .filter(_.nonEmpty)
                              .map(
                                _.split(":") match {
                                    case Array(method, url) => Call(method, url)
                                    case Array(url)         => Call("GET", url)
                                  }
                              )
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

  lazy val redirectUrlWhenDenied: Call =
    allowlistFilterConfig.redirectUrlWhenDenied

  lazy val excludedPaths: Seq[Call] =
    allowlistFilterConfig.excludedPaths

  private val enabled: Boolean =
    config.get[Boolean]("bootstrap.filters.allowlist.enabled")

  private def error(message: String): Future[Result] = {
    logger.error(message)
    Future.successful(InternalServerError)
  }

  protected def response: Result =
    Redirect(redirectUrlWhenDenied)

  protected def excluded(rh: RequestHeader): Boolean = {
    def wildcardMatch(c: Call) =
      c.url.endsWith("/*") && rh.uri.startsWith(c.url.dropRight(2))

    excludedPaths.exists(c => c.method.equalsIgnoreCase(rh.method) && (c.url == rh.uri || wildcardMatch(c)))
  }

  private def processRequest(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] =
    if (excluded(rh))
      f(rh)
    else
      rh.headers.get(trueClient).fold(error(s"No $trueClient Http Header Found in the request"))(ip =>
        if (allowlist.contains(ip))
          f(rh)
        else if (rh.uri == redirectUrlWhenDenied.url)
          error(s"Not allowed. Forwarding to '${rh.uri}' would result in a redirect loop, reconfigure $redirectUrlWhenDenied")
        else
          Future.successful(response)
      )

  override def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] =
    if (enabled)
      processRequest(f)(rh)
    else
      f(rh)
}
