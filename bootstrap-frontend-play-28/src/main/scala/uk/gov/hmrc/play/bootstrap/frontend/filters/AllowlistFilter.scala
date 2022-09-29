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
import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc.{Call, RequestHeader, Result}
import uk.gov.hmrc.allowlist.AkamaiAllowlistFilter

import scala.concurrent.Future

@Singleton
class AllowlistFilter @Inject() (
  config: Configuration,
  override val mat: Materializer
) extends AkamaiAllowlistFilter {

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
      config.get[String]("bootstrap.filters.allowlist.excluded")
        .split(",")
        .toIndexedSeq
        .map(_.trim)
        .filter(_.nonEmpty)
        .map(path => Call("GET", path))
  )

  def loadConfig: AllowlistFilter = {
    allowlistFilterConfig
    this
  }

  override lazy val allowlist: Seq[String] =
    allowlistFilterConfig.allowlist

  @deprecated("Use allowlist instead", "4.0.0")
  def whitelist: Seq[String] =
    allowlist

  override lazy val destination: Call =
    allowlistFilterConfig.destination

  override lazy val excludedPaths: Seq[Call] =
    allowlistFilterConfig.excludedPaths

  private val enabled: Boolean = config.get[Boolean]("bootstrap.filters.allowlist.enabled")

  override def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
    if (enabled) {
      super.apply(f)(rh)
    } else {
      f(rh)
    }
  }
}
