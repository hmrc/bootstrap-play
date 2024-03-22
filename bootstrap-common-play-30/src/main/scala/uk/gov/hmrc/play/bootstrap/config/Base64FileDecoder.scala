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

import com.typesafe.config.{ConfigException, ConfigValue, ConfigValueType}
import play.api.{Configuration, Logger}

import java.io.{File, FileOutputStream}
import java.util.Base64
import scala.util.Try
import scala.util.matching.Regex

object Base64FileDecoder {

  private val logger = Logger(getClass)
  private val encodedFilePattern: Regex = """^(encoded-files\.\w+)\.data$""".r

  private def decodeAndStore(keyBase: String, value: ConfigValue, path: String): Unit = {
    if (value.valueType != ConfigValueType.STRING) {
      throw new ConfigException.BadValue(
        s"$keyBase.data",
        "only strings can be Base64 decoded"
      )
    }

    val raw = value.unwrapped.asInstanceOf[String]
    val decoded: Array[Byte] = Try(Base64.getDecoder.decode(raw)).getOrElse(throw new ConfigException.BadValue(
      s"$keyBase.data",
      "Invalid Base64 string"
    ))

    val file = new File(path)
    file.deleteOnExit()
    val os = new FileOutputStream(file)
    try {
      os.write(decoded)
      os.flush()
    } finally {
      os.close()
      logger.info(s"File successfully created at '$path' containing the decoded content of '$keyBase.data'")
    }
  }

  def decodeAndStoreFilesInConfig(configuration: Configuration): Unit = {
    configuration.entrySet.foreach {
      case (encodedFilePattern(keyBase), value) =>
        val locationKey = s"$keyBase.location"
        configuration.getOptional[String](locationKey) match {
          case Some(path) if !path.startsWith("/tmp") =>
            throw new ConfigException.BadValue(
              locationKey,
              "location must start with '/tmp'"
            )
          case None =>
            throw new ConfigException.BadValue(
              locationKey,
              s"corresponding 'location' is not defined for $keyBase.data"
            )
          case Some(path) =>
            decodeAndStore(keyBase, value, path)
        }
      case _ => // Ignore non-matching entries
    }
  }
}
