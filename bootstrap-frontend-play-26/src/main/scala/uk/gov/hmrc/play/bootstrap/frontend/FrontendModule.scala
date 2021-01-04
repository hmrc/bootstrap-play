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

package uk.gov.hmrc.play.bootstrap.frontend

import play.api.inject.Binding
import play.api.{Configuration, Environment}
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.play.bootstrap.BootstrapModule
import uk.gov.hmrc.play.bootstrap.config.{CryptoValidation, DeprecatedConfigChecker}
import uk.gov.hmrc.play.bootstrap.filters.AuditFilter
import uk.gov.hmrc.play.bootstrap.frontend.filters.{DefaultFrontendAuditFilter, SessionTimeoutFilterConfig}
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.{ApplicationCryptoProvider, DefaultSessionCookieCryptoFilter, SessionCookieCrypto, SessionCookieCryptoFilter, SessionCookieCryptoProvider}
import uk.gov.hmrc.play.bootstrap.frontend.filters.deviceid.{DefaultDeviceIdFilter, DeviceIdFilter}

class FrontendModule extends BootstrapModule {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] =
    super.bindings(environment, configuration) ++ Seq(
      bind[AuditFilter].to[DefaultFrontendAuditFilter],
      bind[ApplicationCrypto].toProvider[ApplicationCryptoProvider],
      bind[SessionCookieCrypto].toProvider[SessionCookieCryptoProvider],
      bind[SessionCookieCryptoFilter].to[DefaultSessionCookieCryptoFilter],
      bind[DeviceIdFilter].to[DefaultDeviceIdFilter],
      bind[SessionTimeoutFilterConfig].toInstance(SessionTimeoutFilterConfig.fromConfig(configuration)),
      bind[CryptoValidation].toSelf.eagerly,
      bind[DeprecatedConfigChecker].toInstance(new DeprecatedConfigChecker(configuration, deprecatedClasses)).eagerly
    )
}