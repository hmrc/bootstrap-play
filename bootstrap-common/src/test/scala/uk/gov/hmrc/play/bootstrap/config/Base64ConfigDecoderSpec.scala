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

package uk.gov.hmrc.play.bootstrap.config

import com.typesafe.config.ConfigException
import org.apache.commons.codec.binary.Base64
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration

trait Base64ConfigDecoderTests extends AnyWordSpec with Matchers {

  def decode(config: (String, Any)*): Configuration

  def aBase64Decoder(): Unit = {

    val quux = Base64.encodeBase64String("quux".getBytes())

    val config = decode(
      "foo"        -> "bar",
      "womble"     -> 7331,
      "baz.base64" -> quux
    )

    "not replace non-encoded values" in {
      config.getOptional[String]("foo") mustBe Some("bar")
      config.getOptional[Int]("womble") mustBe Some(7331)
    }

    "decode encoded values" in {
      config.getOptional[String]("baz") mustBe Some("quux")
    }

    "throw an exception when trying to decode a non-string value" in {

      val exception = intercept[ConfigException.BadValue] {
        decode(
          "spoon.base64" -> 1337
        )
      }

      exception.getMessage mustEqual "hardcoded value: Invalid value at 'spoon.base64': only strings can be Base64 decoded"
    }
  }
}

class Base64ConfigDecoderSpec extends Base64ConfigDecoderTests with Base64ConfigDecoder {

  override def decode(config: (String, Any)*): Configuration =
    decodeConfig(Configuration(config.toSeq: _*))

  ".decode" must {
    behave like aBase64Decoder
  }
}
