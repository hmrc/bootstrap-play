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

package uk.gov.hmrc.play.bootstrap.backend.filters

import com.kenshoo.play.metrics.MetricsFilter
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.http.DefaultHttpFilters
import uk.gov.hmrc.play.bootstrap.filters.{AuditFilter, CacheControlFilter, LoggingFilter, MDCFilter}


@deprecated("Config setting play.http.filters = \"uk.gov.hmrc.play.bootstrap.backend.filters.BackendFilters\" is no longer required. Bootstrap filters are now configured via backend.conf", "4.0.0")
@Singleton
class BackendFilters @Inject()(
  metricsFilter: MetricsFilter,
  auditFilter  : AuditFilter,
  loggingFilter: LoggingFilter,
  cacheFilter  : CacheControlFilter,
  mdcFilter    : MDCFilter
) extends DefaultHttpFilters(
  metricsFilter,
  auditFilter,
  loggingFilter,
  cacheFilter,
  mdcFilter
) {
  private val logger = Logger(getClass)
  logger.warn("play.http.filters = \"uk.gov.hmrc.play.bootstrap.backend.filters.BackendFilters\" is no longer required and can be removed. Filters are configured using play's default filter system: https://www.playframework.com/documentation/2.8.x/Filters#Default-Filters")
}
