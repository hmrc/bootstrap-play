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
import uk.gov.hmrc.crypto.{Crypted, Decrypter, Encrypter, PlainBytes, PlainContent, PlainText, SymmetricCryptoFactory}

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

  /** Should only be used to encrypt/decrypt the cookie.
    *
    * It is shared by all services.
    *
    * This is a platform key, and should not be used for any other use-case since it may be rotated at any time.
    */
  lazy val SessionCookieCrypto =
    SymmetricCryptoFactory.aesGcmCryptoFromConfig(baseConfigKey = "cookie.encryption", configuration.underlying)

  /** Should only be used for SSO with the Portal.
    *
    * It is shared by all services.
    *
    * This is a platform key, and should not be used for any other use-case since it may be rotated at any time.
    *
    * Reads AES-GCM with a fallback to AES. Writes AES-GCM when `sso.encryption.useGcm = true` (default: false).
    */
  lazy val SsoPayloadCrypto: Encrypter with Decrypter =
    aesGcmWithAesFallback(
      baseConfigKey  = "sso.encryption",
      useGcmForWrite = configuration.getOptional[Boolean]("sso.encryption.useGcm").getOrElse(false)
    )

  /** Can be used to encrypt query parameters - e.g. for callbacks and redirects.
    *
    * By default it is shared by all services, but it can be overridden if required to be private to the service.
    *
    * Given by default it is provided by the platform, it should be assumed it may be rotated at any time, and not
    * used for storing data.
    *
    * Reads AES-GCM with a fallback to AES. Writes AES-GCM when `queryParameter.encryption.useGcm = true` (default: false).
    */
  lazy val QueryParameterCrypto: Encrypter with Decrypter =
    aesGcmWithAesFallback(
      baseConfigKey  = "queryParameter.encryption",
      useGcmForWrite = configuration.getOptional[Boolean]("queryParameter.encryption.useGcm").getOrElse(false)
    )

  /** Creates a composite crypto that decrypts with AES-GCM (falling back to AES on failure) and
    * encrypts with either AES-GCM or AES depending on [[useGcmForWrite]].
    */
  private def aesGcmWithAesFallback(baseConfigKey: String, useGcmForWrite: Boolean): Encrypter with Decrypter = {
    val aesCrypto = SymmetricCryptoFactory.aesCryptoFromConfig(baseConfigKey, configuration.underlying)
    val gcmCrypto = SymmetricCryptoFactory.aesGcmCryptoFromConfig(baseConfigKey, configuration.underlying)
    new Encrypter with Decrypter {
      override def encrypt(plain: PlainContent): Crypted =
        if (useGcmForWrite) gcmCrypto.encrypt(plain)
        else                aesCrypto.encrypt(plain)

      override def decrypt(reversiblyEncrypted: Crypted): PlainText =
        if (useGcmForWrite)
          try  gcmCrypto.decrypt(reversiblyEncrypted)
          catch { case _: Exception => aesCrypto.decrypt(reversiblyEncrypted) }
        else
          try  aesCrypto.decrypt(reversiblyEncrypted)
          catch { case _: Exception => gcmCrypto.decrypt(reversiblyEncrypted) }

      override def decryptAsBytes(reversiblyEncrypted: Crypted): PlainBytes =
        if (useGcmForWrite)
          try  gcmCrypto.decryptAsBytes(reversiblyEncrypted)
          catch { case _: Exception => aesCrypto.decryptAsBytes(reversiblyEncrypted) }
        else
          try  aesCrypto.decryptAsBytes(reversiblyEncrypted)
          catch { case _: Exception => gcmCrypto.decryptAsBytes(reversiblyEncrypted) }
    }
  }

  def verifyConfiguration(): Unit = {
    SessionCookieCrypto
    QueryParameterCrypto
    SsoPayloadCrypto
  }
}

@annotation.nowarn("msg=deprecated")
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
