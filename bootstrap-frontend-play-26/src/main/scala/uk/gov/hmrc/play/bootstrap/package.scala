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

package uk.gov.hmrc.play

import akka.stream.Materializer
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.frontend.filters.{AllowlistFilter, SessionIdFilter}


package bootstrap {

  @deprecated("Use uk.gov.hmrc.play.bootstrap.frontend.FrontendModule instead", "2.12.0")
  class FrontendModule extends uk.gov.hmrc.play.bootstrap.frontend.FrontendModule

  package object controller {
    @deprecated("Use uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController instead", "2.12.0")
    type FrontendBaseController = uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

    @deprecated("Use uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController instead", "2.12.0")
    type FrontendController = uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

    @deprecated("Use uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider instead", "2.12.0")
    type FrontendHeaderCarrierProvider = uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
  }

  package filters {

    import play.api.http.EnabledFilters

    @deprecated("Use uk.gov.hmrc.play.bootstrap.frontend.filters.FrontendFilters instead", "2.12.0")
    @Singleton
    class FrontendFilters @Inject()(
      configuration            : Configuration,
      allowlistFilter          : AllowlistFilter,
      sessionIdFilter          : SessionIdFilter,
      enabledFilters           : EnabledFilters
    ) extends uk.gov.hmrc.play.bootstrap.frontend.filters.FrontendFilters(
      configuration,
      allowlistFilter,
      sessionIdFilter,
      enabledFilters
    )

    @deprecated("Use uk.gov.hmrc.play.bootstrap.frontend.filters.AllowlistFilter instead", "4.0.0")
    @Singleton
    class WhitelistFilter @Inject()(
      config: Configuration,
      mat: Materializer
    ) extends AllowlistFilter(config, mat)

    package frontend {
      package object crypto {
        @deprecated("Use uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.ApplicationCryptoProvider instead", "2.12.0")
        type ApplicationCryptoProvider = uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.ApplicationCryptoProvider

        @deprecated("Use uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCrypto instead", "2.12.0")
        type SessionCookieCrypto = uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCrypto

        @deprecated("Use uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoProvider instead", "2.12.0")
        type SessionCookieCryptoProvider = uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoProvider

        @deprecated("Use uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.CryptoImplicits instead", "2.12.0")
        type CryptoImplicits = uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.CryptoImplicits

        @deprecated("Use uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter instead", "2.12.0")
        type SessionCookieCryptoFilter = uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter

        @deprecated("Use uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.DefaultSessionCookieCryptoFilter instead", "2.12.0")
        type DefaultSessionCookieCryptoFilter = uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.DefaultSessionCookieCryptoFilter
      }

      package object deviceid {
        @deprecated("Use uk.gov.hmrc.play.bootstrap.frontend.filters.deviceid.DefaultDeviceIdFilter instead", "2.12.0")
        type DefaultDeviceIdFilter = uk.gov.hmrc.play.bootstrap.frontend.filters.deviceid.DefaultDeviceIdFilter

        @deprecated("Use uk.gov.hmrc.play.bootstrap.frontend.filters.deviceid.DeviceFingerprint instead", "2.12.0")
        type DeviceFingerprint = uk.gov.hmrc.play.bootstrap.frontend.filters.deviceid.DeviceFingerprint

        @deprecated("Use uk.gov.hmrc.play.bootstrap.frontend.filters.deviceid.DeviceFingerprint instead", "2.12.0")
        val DeviceFingerprint = uk.gov.hmrc.play.bootstrap.frontend.filters.deviceid.DeviceFingerprint

        @deprecated("Use uk.gov.hmrc.play.bootstrap.frontend.filters.deviceid.DeviceId instead", "2.12.0")
        type DeviceId = uk.gov.hmrc.play.bootstrap.frontend.filters.deviceid.DeviceId

        @deprecated("Use uk.gov.hmrc.play.bootstrap.frontend.filters.deviceid.DeviceId instead", "2.12.0")
        val DeviceId = uk.gov.hmrc.play.bootstrap.frontend.filters.deviceid.DeviceId

        @deprecated("Use uk.gov.hmrc.play.bootstrap.frontend.filters.deviceid.DeviceIdCookie instead", "2.12.0")
        type DeviceIdCookie = uk.gov.hmrc.play.bootstrap.frontend.filters.deviceid.DeviceIdCookie

        @deprecated("Use uk.gov.hmrc.play.bootstrap.frontend.filters.deviceid.DeviceIdFilter instead", "2.12.0")
        type DeviceIdFilter = uk.gov.hmrc.play.bootstrap.frontend.filters.deviceid.DeviceIdFilter
      }
    }

    package object frontend {
      @deprecated("Use uk.gov.hmrc.play.bootstrap.frontend.filters.FrontendAuditFilter instead", "2.12.0")
      type FrontendAuditFilter = uk.gov.hmrc.play.bootstrap.frontend.filters.FrontendAuditFilter

      @deprecated("Use uk.gov.hmrc.play.bootstrap.frontend.filters.DefaultFrontendAuditFilter instead", "2.12.0")
      type DefaultFrontendAuditFilter = uk.gov.hmrc.play.bootstrap.frontend.filters.DefaultFrontendAuditFilter

      @deprecated("Use uk.gov.hmrc.play.bootstrap.frontend.filters.HeadersFilter instead", "2.12.0")
      type HeadersFilter = uk.gov.hmrc.play.bootstrap.frontend.filters.HeadersFilter

      @deprecated("Use uk.gov.hmrc.play.bootstrap.frontend.filters.SessionTimeoutFilterConfig instead", "2.12.0")
      type SessionTimeoutFilterConfig = uk.gov.hmrc.play.bootstrap.frontend.filters.SessionTimeoutFilterConfig

      @deprecated("Use uk.gov.hmrc.play.bootstrap.frontend.filters.SessionTimeoutFilterConfig instead", "2.12.0")
      val SessionTimeoutFilterConfig = uk.gov.hmrc.play.bootstrap.frontend.filters.SessionTimeoutFilterConfig

      @deprecated("Use uk.gov.hmrc.play.bootstrap.frontend.filters.SessionTimeoutFilter instead", "2.12.0")
      type SessionTimeoutFilter = uk.gov.hmrc.play.bootstrap.frontend.filters.SessionTimeoutFilter

      @deprecated("Use uk.gov.hmrc.play.bootstrap.frontend.filters.SessionTimeoutFilter instead", "2.12.0")
      val SessionTimeoutFilter = uk.gov.hmrc.play.bootstrap.frontend.filters.SessionTimeoutFilter
    }
  }

  package object http {
    @deprecated("Use uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler instead", "2.12.0")
    type FrontendErrorHandler = uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler

    @deprecated("Use uk.gov.hmrc.play.bootstrap.frontend.http.ApplicationException instead", "2.12.0")
    type ApplicationException = uk.gov.hmrc.play.bootstrap.frontend.http.ApplicationException

    @deprecated("Use uk.gov.hmrc.http.HttpClient instead", "2.15.0")
    type HttpClient = uk.gov.hmrc.http.HttpClient
  }

  package object frontend {
    val deprecatedClasses: Map[String, String] =
      Map(
        classOf[uk.gov.hmrc.play.bootstrap.FrontendModule].getName                                           -> classOf[uk.gov.hmrc.play.bootstrap.frontend.FrontendModule].getName,
        classOf[uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController].getName                        -> classOf[uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController].getName,
        classOf[uk.gov.hmrc.play.bootstrap.controller.FrontendController].getName                            -> classOf[uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController].getName,
        classOf[uk.gov.hmrc.play.bootstrap.controller.FrontendHeaderCarrierProvider].getName                 -> classOf[uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider].getName,
        classOf[uk.gov.hmrc.play.bootstrap.filters.FrontendFilters].getName                                  -> classOf[uk.gov.hmrc.play.bootstrap.frontend.filters.FrontendFilters].getName,
        classOf[uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.ApplicationCryptoProvider].getName        -> classOf[uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.ApplicationCryptoProvider].getName,
        classOf[uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCrypto].getName              -> classOf[uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCrypto].getName,
        classOf[uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoProvider].getName      -> classOf[uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoProvider].getName,
        classOf[uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.CryptoImplicits].getName                  -> classOf[uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.CryptoImplicits].getName,
        classOf[uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoFilter].getName        -> classOf[uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter].getName,
        classOf[uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.DefaultSessionCookieCryptoFilter].getName -> classOf[uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.DefaultSessionCookieCryptoFilter].getName,
        classOf[uk.gov.hmrc.play.bootstrap.filters.frontend.deviceid.DefaultDeviceIdFilter].getName          -> classOf[uk.gov.hmrc.play.bootstrap.frontend.filters.deviceid.DefaultDeviceIdFilter].getName,
        classOf[uk.gov.hmrc.play.bootstrap.filters.frontend.deviceid.DeviceFingerprint].getName              -> classOf[uk.gov.hmrc.play.bootstrap.frontend.filters.deviceid.DeviceFingerprint].getName,
        classOf[uk.gov.hmrc.play.bootstrap.filters.frontend.deviceid.DeviceId].getName                       -> classOf[uk.gov.hmrc.play.bootstrap.frontend.filters.deviceid.DeviceId].getName,
        classOf[uk.gov.hmrc.play.bootstrap.filters.frontend.deviceid.DeviceIdCookie].getName                 -> classOf[uk.gov.hmrc.play.bootstrap.frontend.filters.deviceid.DeviceIdCookie].getName,
        classOf[uk.gov.hmrc.play.bootstrap.filters.frontend.deviceid.DeviceIdFilter].getName                 -> classOf[uk.gov.hmrc.play.bootstrap.frontend.filters.deviceid.DeviceIdFilter].getName,
        classOf[uk.gov.hmrc.play.bootstrap.filters.frontend.FrontendAuditFilter].getName                     -> classOf[uk.gov.hmrc.play.bootstrap.frontend.filters.FrontendAuditFilter].getName,
        classOf[uk.gov.hmrc.play.bootstrap.filters.frontend.DefaultFrontendAuditFilter].getName              -> classOf[uk.gov.hmrc.play.bootstrap.frontend.filters.DefaultFrontendAuditFilter].getName,
        classOf[uk.gov.hmrc.play.bootstrap.filters.frontend.HeadersFilter].getName                           -> classOf[uk.gov.hmrc.play.bootstrap.frontend.filters.HeadersFilter].getName,
        classOf[uk.gov.hmrc.play.bootstrap.filters.frontend.SessionTimeoutFilterConfig].getName              -> classOf[uk.gov.hmrc.play.bootstrap.frontend.filters.SessionTimeoutFilterConfig].getName,
        classOf[uk.gov.hmrc.play.bootstrap.filters.frontend.SessionTimeoutFilter].getName                    -> classOf[uk.gov.hmrc.play.bootstrap.frontend.filters.SessionTimeoutFilter].getName,
        classOf[uk.gov.hmrc.play.bootstrap.http.FrontendErrorHandler].getName                                -> classOf[uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler].getName,
        classOf[uk.gov.hmrc.play.bootstrap.http.ApplicationException].getName                                -> classOf[uk.gov.hmrc.play.bootstrap.frontend.http.ApplicationException].getName
      )
  }
}
