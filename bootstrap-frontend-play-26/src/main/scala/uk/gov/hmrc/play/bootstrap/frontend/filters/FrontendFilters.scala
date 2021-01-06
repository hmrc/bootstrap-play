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

package uk.gov.hmrc.play.bootstrap.frontend.filters

import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Logger}
import play.api.http.{EnabledFilters, HttpFilters}
import play.api.mvc.EssentialFilter
import play.filters.csrf.CSRFFilter
import play.filters.headers.SecurityHeadersFilter
import scala.reflect.ClassTag

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

  override val filters: Seq[EssentialFilter] = {
    val filters =
      removeWhenDisabled[SecurityHeadersFilter]("security.headers.filter.enabled")(
        removeWhenDisabled[CSRFFilter]("bootstrap.filters.csrf.enabled")(
          addWhenEnabled("bootstrap.filters.allowlist.enabled", allowlistFilter.loadConfig)(
            addWhenEnabled("bootstrap.filters.sessionId.enabled", sessionIdFilter)(
              enabledFilters.filters
            )
          )
        )
      )
    logger.info(s"EnabledFilters has been amended to ${filters.map(_.getClass.getName).mkString("\n  ", "\n  ", "\n")}")
    filters
  }

  private def addWhenEnabled[T](key: String, filter: => EssentialFilter)(filters: Seq[EssentialFilter]): Seq[EssentialFilter] =
    if (configuration.get[Boolean](key))
      filters :+ filter
    else
      filters

  private def removeWhenDisabled[T : ClassTag](key: String)(filters: Seq[EssentialFilter]): Seq[EssentialFilter] =
    if (!configuration.get[Boolean](key))
      filters.filter {
        case f: T => false
        case f    => true
      }
    else
      filters
}
