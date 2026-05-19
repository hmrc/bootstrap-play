/*
 * Copyright 2026 HM Revenue & Customs
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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import uk.gov.hmrc.crypto.{PlainText, SymmetricCryptoFactory}

class ApplicationCryptoSpec extends AnyWordSpec with Matchers {

  private val testKey = "MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI="

  private def configWith(extraEntries: (String, Any)*): Configuration =
    Configuration(
      (Seq(
        "cookie.encryption.key"          -> testKey,
        "sso.encryption.key"             -> testKey,
        "queryParameter.encryption.key"  -> testKey
      ) ++ extraEntries): _*
    )

  private val aesEncrypter =
    SymmetricCryptoFactory.aesCryptoFromConfig("sso.encryption", configWith().underlying)

  private val gcmEncrypter =
    SymmetricCryptoFactory.aesGcmCryptoFromConfig("sso.encryption", configWith().underlying)

  private val plainValue = PlainText("sensitive-payload")

  "SsoPayloadCrypto" when {

    "useGcm is false (default)" should {

      val crypto = new ApplicationCrypto(configWith()).SsoPayloadCrypto

      "encrypt with AES" in {
        val ciphertext = crypto.encrypt(plainValue)
        aesEncrypter.decrypt(ciphertext) shouldBe plainValue
      }

      "decrypt a value encrypted with AES (primary read path)" in {
        val ciphertext = aesEncrypter.encrypt(plainValue)
        crypto.decrypt(ciphertext) shouldBe plainValue
      }

      "decrypt a value encrypted with AES-GCM (fallback read path)" in {
        val ciphertext = gcmEncrypter.encrypt(plainValue)
        crypto.decrypt(ciphertext) shouldBe plainValue
      }
    }

    "useGcm is true" should {

      val crypto = new ApplicationCrypto(configWith("sso.encryption.useGcm" -> true)).SsoPayloadCrypto

      "encrypt with AES-GCM" in {
        val ciphertext = crypto.encrypt(plainValue)
        gcmEncrypter.decrypt(ciphertext) shouldBe plainValue
      }

      "decrypt a value encrypted with AES-GCM (primary read path)" in {
        val ciphertext = gcmEncrypter.encrypt(plainValue)
        crypto.decrypt(ciphertext) shouldBe plainValue
      }

      "decrypt a legacy value encrypted with AES (fallback read path)" in {
        val ciphertext = aesEncrypter.encrypt(plainValue)
        crypto.decrypt(ciphertext) shouldBe plainValue
      }
    }
  }

  "QueryParameterCrypto" when {

    val aesQpEncrypter = SymmetricCryptoFactory.aesCryptoFromConfig("queryParameter.encryption", configWith().underlying)
    val gcmQpEncrypter = SymmetricCryptoFactory.aesGcmCryptoFromConfig("queryParameter.encryption", configWith().underlying)

    "useGcm is false (default)" should {

      val crypto = new ApplicationCrypto(configWith()).QueryParameterCrypto

      "encrypt with AES" in {
        val ciphertext = crypto.encrypt(plainValue)
        aesQpEncrypter.decrypt(ciphertext) shouldBe plainValue
      }

      "decrypt a value encrypted with AES (primary read path)" in {
        val ciphertext = aesQpEncrypter.encrypt(plainValue)
        crypto.decrypt(ciphertext) shouldBe plainValue
      }

      "decrypt a value encrypted with AES-GCM (fallback read path)" in {
        val ciphertext = gcmQpEncrypter.encrypt(plainValue)
        crypto.decrypt(ciphertext) shouldBe plainValue
      }
    }

    "useGcm is true" should {

      val crypto = new ApplicationCrypto(configWith("queryParameter.encryption.useGcm" -> true)).QueryParameterCrypto

      "encrypt with AES-GCM" in {
        val ciphertext = crypto.encrypt(plainValue)
        gcmQpEncrypter.decrypt(ciphertext) shouldBe plainValue
      }

      "decrypt a value encrypted with AES-GCM (primary read path)" in {
        val ciphertext = gcmQpEncrypter.encrypt(plainValue)
        crypto.decrypt(ciphertext) shouldBe plainValue
      }

      "decrypt a legacy value encrypted with AES (fallback read path)" in {
        val ciphertext = aesQpEncrypter.encrypt(plainValue)
        crypto.decrypt(ciphertext) shouldBe plainValue
      }
    }
  }
}




