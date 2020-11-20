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

import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Logger}
import play.api.http.{EnabledFilters, HttpFilters}
import play.api.mvc.EssentialFilter

@deprecated("remove config setting play.http.filters = \"uk.gov.hmrc.play.bootstrap.frontend.filters.FrontendFilters\" is no longer required. Bootstrap filters are now configured via frontend.conf", "2.12.0")
@Singleton
class FrontendFilters @Inject()(
  configuration            : Configuration,
  allowlistFilter          : AllowlistFilter,
  sessionIdFilter          : SessionIdFilter,
  enabledFilters           : EnabledFilters
) extends HttpFilters {

  private val logger = Logger(getClass)
  logger.warn("play.http.filters = \"uk.gov.hmrc.play.bootstrap.frontend.filters.FrontendFilter\" is no longer required and can be removed. Filters are configured using play's default filter system: https://www.playframework.com/documentation/2.7.x/Filters#Default-Filters")

  override val filters: Seq[EssentialFilter] =
    enabledFilters.filters ++
    whenEnabled("bootstrap.filters.allowlist.enabled", allowlistFilter.loadConfig) ++
    whenEnabled("bootstrap.filters.sessionId.enabled", sessionIdFilter)

  private def whenEnabled(key: String, filter: => EssentialFilter): Seq[EssentialFilter] = {
    logger.warn(s"""config key $key is deprecated, please configure this filter via play.filters.enabled instead""")
    if (configuration.get[Boolean](key)) Seq(filter)
    else Seq.empty
  }
}
