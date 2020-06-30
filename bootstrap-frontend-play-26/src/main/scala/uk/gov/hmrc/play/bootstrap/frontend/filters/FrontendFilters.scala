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

import com.kenshoo.play.metrics.MetricsFilter
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.http.HttpFilters
import play.filters.csrf.CSRFFilter
import play.filters.headers.SecurityHeadersFilter
import uk.gov.hmrc.play.bootstrap.filters.{AuditFilter, CacheControlFilter, LoggingFilter, MDCFilter}
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter
import uk.gov.hmrc.play.bootstrap.frontend.filters.deviceid.DeviceIdFilter

@Singleton
class FrontendFilters @Inject()(
  configuration            : Configuration,
  loggingFilter            : LoggingFilter,
  headersFilter            : HeadersFilter,
  securityFilter           : SecurityHeadersFilter,
  frontendAuditFilter      : AuditFilter,
  metricsFilter            : MetricsFilter,
  deviceIdFilter           : DeviceIdFilter,
  csrfFilter               : CSRFFilter,
  sessionCookieCryptoFilter: SessionCookieCryptoFilter,
  sessionTimeoutFilter     : SessionTimeoutFilter,
  cacheControlFilter       : CacheControlFilter,
  mdcFilter                : MDCFilter,
  sessionIdFilter          : SessionIdFilter
) extends HttpFilters {

  val frontendFilters = Seq(
    metricsFilter,
    sessionCookieCryptoFilter,
    headersFilter,
    deviceIdFilter,
    loggingFilter,
    frontendAuditFilter,
    sessionTimeoutFilter,
    csrfFilter, // this one is excluded by some clients - should it have an "enabled" config?
    cacheControlFilter,
    mdcFilter,
    sessionIdFilter // is documentation enough to get clients to not add sessionId filter twice?
      // can we add it with reference.conf?
  )



  lazy val enableSecurityHeaderFilter: Boolean =
    configuration.getOptional[Boolean]("security.headers.filter.enabled").getOrElse(true)

  override val filters =
    if (enableSecurityHeaderFilter)
      Seq(securityFilter) ++ frontendFilters
    else
      frontendFilters
}
