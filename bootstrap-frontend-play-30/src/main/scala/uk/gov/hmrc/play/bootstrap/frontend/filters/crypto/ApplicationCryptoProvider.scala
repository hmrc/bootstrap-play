/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.play.bootstrap.frontend.filters.crypto

import play.api.Configuration
import uk.gov.hmrc.crypto.SymmetricCryptoFactory

import javax.inject.{Inject, Provider}

class ApplicationCryptoProvider @Inject()(
  configuration: Configuration
) extends Provider[ApplicationCrypto] {

  private val crypto =
    new ApplicationCrypto(configuration)
  crypto.verifyConfiguration()

  def get(): ApplicationCrypto =
    crypto
}

class ApplicationCrypto @Inject()(configuration: Configuration) {

  /** Used to encrypt the cookie.
    * It is shared by all services.
    * This is a platform key, and should not be used for any other use-case since it may be rotated at any time.
    */
  lazy val SessionCookieCrypto =
    SymmetricCryptoFactory.aesGcmCryptoFromConfig(baseConfigKey = "cookie.encryption", configuration.underlying)

  /** Used for SSO with with the Portal.
    * It is shared by all services.
    * This is a platform key, and should not be used for any other use-case since it may be rotated at any time.
    */
  lazy val SsoPayloadCrypto =
    SymmetricCryptoFactory.aesCryptoFromConfig(baseConfigKey = "sso.encryption", configuration.underlying)

  /** Used to encrypt query parameters. It can be used for path parameters and json payloads too.
    * It is shared by all services.
    * This is a platform key, and should not be used for any other use-case since it may be rotated at any time.
    */
  lazy val QueryParameterCrypto =
    SymmetricCryptoFactory.aesCryptoFromConfig(baseConfigKey = "queryParameter.encryption", configuration.underlying)

  def verifyConfiguration(): Unit = {
    SessionCookieCrypto
    QueryParameterCrypto
    SsoPayloadCrypto
  }
}

class DeprecatedApplicationCryptoProvider @Inject()(
  configuration: Configuration
) extends Provider[uk.gov.hmrc.crypto.ApplicationCrypto] {

  private val logger = play.api.Logger(getClass)

  private val crypto =
    new uk.gov.hmrc.crypto.ApplicationCrypto(configuration.underlying)
  crypto.verifyConfiguration()

  def get(): uk.gov.hmrc.crypto.ApplicationCrypto = {
    logger.warn("uk.gov.hmrc.crypto.ApplicationCrypto is deprecated. Inject uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.AppliationCrypto instead.")
    crypto
  }
}
