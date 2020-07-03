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
import com.google.inject.Inject
import play.api.Configuration
import play.api.mvc.Call
import uk.gov.hmrc.whitelist.AkamaiWhitelistFilter

class WhitelistFilter @Inject() (
  config: Configuration,
  override val mat: Materializer
) extends AkamaiWhitelistFilter {

  case class WhitelistFilterConfig(
    whitelist: Seq[String],
    destination: Call,
    excludedPaths: Seq[Call]
  )

  private lazy val whitelistFilterConfig = WhitelistFilterConfig(
    whitelist =
      config.get[String]("bootstrap.filters.whitelist.ips")
        .split(",")
        .map(_.trim)
        .filter(_.nonEmpty),

    destination = {
      val path = config.get[String]("bootstrap.filters.whitelist.destination")
      Call("GET", path)
    },

    excludedPaths =
      config.get[String]("bootstrap.filters.whitelist.excluded")
        .split(",")
        .map(_.trim)
        .filter(_.nonEmpty)
        .map(path => Call("GET", path))
  )

  def loadConfig: WhitelistFilter = {
    whitelistFilterConfig
    this
  }

  override lazy val whitelist: Seq[String] =
    whitelistFilterConfig.whitelist

  override lazy val destination: Call =
    whitelistFilterConfig.destination

  override lazy val excludedPaths: Seq[Call] =
    whitelistFilterConfig.excludedPaths
}