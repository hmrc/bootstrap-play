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

package uk.gov.hmrc.play.bootstrap.backend.filters

import com.kenshoo.play.metrics.MetricsFilter
import javax.inject.{Inject, Singleton}
import play.api.http.{EnabledFilters, HttpFilters}
import play.api.mvc.EssentialFilter
import uk.gov.hmrc.play.bootstrap.filters.{AuditFilter, CacheControlFilter, LoggingFilter, MDCFilter}

@Singleton
class BackendFilters @Inject()(
  defaultFilters: EnabledFilters,
  metricsFilter : MetricsFilter,
  auditFilter   : AuditFilter,
  loggingFilter : LoggingFilter,
  cacheFilter   : CacheControlFilter,
  mdcFilter     : MDCFilter
) extends HttpFilters {

  override val filters: Seq[EssentialFilter] =
    defaultFilters.filters :+
    metricsFilter :+
    auditFilter   :+
    loggingFilter :+
    cacheFilter   :+
    mdcFilter
}
