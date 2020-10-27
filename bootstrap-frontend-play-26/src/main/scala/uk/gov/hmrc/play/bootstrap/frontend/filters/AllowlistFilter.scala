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

package uk.gov.hmrc.play.bootstrap.frontend.filters

import akka.stream.Materializer
import com.google.inject.{Inject, Singleton}
import play.api.{Configuration, ConfigLoader, Logger}
import play.api.mvc.Call
import uk.gov.hmrc.allowlist.AkamaiAllowlistFilter

@Singleton
class AllowlistFilter @Inject() (
  config: Configuration,
  override val mat: Materializer
) extends AkamaiAllowlistFilter {

  case class AllowlistFilterConfig(
    allowlist: Seq[String],
    destination: Call,
    excludedPaths: Seq[Call]
  )

  private lazy val allowlistFilterConfig = AllowlistFilterConfig(
    allowlist =
      ConfigUtil.getDeprecated[String](
          config,
          "bootstrap.filters.allowlist.ips",
          "bootstrap.filters.whitelist.ips"
        )
        .split(",")
        .map(_.trim)
        .filter(_.nonEmpty),

    destination = {
      val path = ConfigUtil.getDeprecated[String](
          config,
          "bootstrap.filters.allowlist.destination",
          "bootstrap.filters.whitelist.destination"
        )
      Call("GET", path)
    },

    excludedPaths =
      ConfigUtil.getDeprecated[String](
          config,
          "bootstrap.filters.allowlist.excluded",
          "bootstrap.filters.whitelist.excluded"
        )
        .split(",")
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

  @deprecated("Use allowlist", "4.0.0")
  def whitelist: Seq[String] =
    allowlist

  override lazy val destination: Call =
    allowlistFilterConfig.destination

  override lazy val excludedPaths: Seq[Call] =
    allowlistFilterConfig.excludedPaths
}

private[filters] object ConfigUtil {

  private val logger = Logger(getClass)

  private[filters] def getDeprecated[A](config: Configuration, k: String, deprecated: String)(implicit loader: ConfigLoader[A]): A =
    getOptionalDeprecated(config, k, deprecated)
      .getOrElse(throw config.globalError(s"Missing configuration key: ['$k']"))

  private[filters] def getOptionalDeprecated[A](config: Configuration, k: String, deprecated: String)(implicit loader: ConfigLoader[A]): Option[A] =
    config.getOptional[A](k)
      .orElse {
        val v = config.getOptional[A](deprecated)
        if (v.isDefined)
          logger.warn(s"Configuration key '$deprecated' is deprecated. Use '$k' instead.")
        v
      }

}
