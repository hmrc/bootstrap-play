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

  override lazy val whitelist: Seq[String] =
    config.get[String]("filters.whitelist.ips")
      .split(",")
      .map(_.trim)
      .filter(_.nonEmpty)

  override lazy val destination: Call = {
    val path = config.get[String]("filters.whitelist.destination")
    Call("GET", path)
  }

  override lazy val excludedPaths: Seq[Call] =
    config.get[String]("filters.whitelist.excluded")
      .split(",")
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(path => Call("GET", path))
}