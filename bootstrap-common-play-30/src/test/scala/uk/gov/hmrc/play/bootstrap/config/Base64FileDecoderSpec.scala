/*
 * Copyright 2024 HM Revenue & Customs
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
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration

import java.util.Base64
import scala.io.{BufferedSource, Source}

class Base64FileDecoderSpec extends AnyWordSpec with Matchers with OptionValues {

  "decodeAndStoreFilesInConfig" should {
    "decode the file and write it to the supplied location" in {
      val content  = "Hello world!"
      val location = "/tmp/file1.tmp"
      val configuration: Configuration = Configuration(
        "encoded-files.file1.data"     -> Base64.getEncoder.encodeToString(content.getBytes("UTF-8")),
        "encoded-files.file1.location" -> location
      )

      Base64FileDecoder.decodeAndStoreFilesInConfig(configuration)

      val source: BufferedSource = Source.fromFile(location)
      try {
        val fileContent: String = source.getLines.mkString("")
        fileContent shouldBe content
      } finally {
        source.close()
      }
    }

    "throw an exception when location is not provided" in {
      val content = "Hello world!"
      val configuration: Configuration = Configuration(
        "encoded-files.file1.data"     -> Base64.getEncoder.encodeToString(content.getBytes("UTF-8")),
      )

      val thrown: ConfigException.BadValue = intercept[ConfigException.BadValue] {
        Base64FileDecoder.decodeAndStoreFilesInConfig(configuration)
      }

      thrown.getMessage should include("corresponding 'location' is not defined for encoded-files.file1.data")
    }

    "throw an exception when location does not start with '/tmp'" in {
      val content = "Hello world!"
      val configuration: Configuration = Configuration(
        "encoded-files.file1.data"     -> Base64.getEncoder.encodeToString(content.getBytes("UTF-8")),
        "encoded-files.file1.location" -> "file1.tmp"
      )

      val thrown: ConfigException.BadValue = intercept[ConfigException.BadValue] {
        Base64FileDecoder.decodeAndStoreFilesInConfig(configuration)
      }

      thrown.getMessage should include("location must start with '/tmp'")
    }

    "throw an exception when it's not a base64 encoded string" in {
      val configuration: Configuration = Configuration(
        "encoded-files.file1.data"     -> 123,
        "encoded-files.file1.location" -> "/tmp/file1.tmp"
      )

      val thrown: ConfigException.BadValue = intercept[ConfigException.BadValue] {
        Base64FileDecoder.decodeAndStoreFilesInConfig(configuration)
      }

      thrown.getMessage should include("only strings can be Base64 decoded")
    }

    "throw an exception when it's invalid base64" in {
      val configuration: Configuration = Configuration(
        "encoded-files.file1.data"     -> "InvalidBase64String==",
        "encoded-files.file1.location" -> "/tmp/file1.tmp"
      )

      val thrown: ConfigException.BadValue = intercept[ConfigException.BadValue] {
        Base64FileDecoder.decodeAndStoreFilesInConfig(configuration)
      }

      thrown.getMessage should include("Invalid Base64 string")
    }
  }
}
