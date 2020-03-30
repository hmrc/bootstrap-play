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

package uk.gov.hmrc.play.bootstrap.config

import com.typesafe.config.{ConfigException, ConfigValue, ConfigValueType}
import org.apache.commons.codec.binary.Base64
import play.api.Configuration

trait Base64ConfigDecoder {

  private val suffix: String = ".base64"

  private def decode(value: String): String =
    new String(Base64.decodeBase64(value))

  private def decode(value: ConfigValue): Option[Any] = {

    val raw = value.unwrapped

    if (value.valueType == ConfigValueType.STRING) {
      Some(decode(raw.asInstanceOf[String]))
    } else {
      None
    }
  }

  private def decode(key: String, value: ConfigValue): (String, Any) =
    decode(value).map(key.dropRight(suffix.length) -> _).getOrElse {
      throw new ConfigException.BadValue(
        value.origin,
        key,
        "only strings can be Base64 decoded"
      )
    }

  protected def decodeConfig(configuration: Configuration): Configuration = {

    val base64Conf = configuration.entrySet.filter {
      case (key, _) =>
        key.endsWith(suffix)
    }

    val newConfig = base64Conf.foldLeft(Map.empty[String, Any]) {
      case (config, (key, value)) =>
        config + decode(key, value)
    }

    val nonBase64Conf = base64Conf.toMap.keys.foldLeft(configuration.underlying) {
      case (config, key) =>
        config.withoutPath(key.dropRight(suffix.length))
    }

    Configuration(newConfig.toSeq: _*) ++ Configuration(nonBase64Conf)
  }
}
