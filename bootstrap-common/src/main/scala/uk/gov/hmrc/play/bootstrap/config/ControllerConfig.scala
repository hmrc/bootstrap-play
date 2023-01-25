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

package uk.gov.hmrc.play.bootstrap.config

import com.typesafe.config.ConfigObject
import play.api.Configuration

case class ControllerConfig(logging: Boolean = true, auditing: Boolean = true)

object ControllerConfig {

  def fromConfig(configuration: Configuration): ControllerConfig = {
    val logging  = configuration.getOptional[Boolean]("needsLogging").getOrElse(true)
    val auditing = configuration.getOptional[Boolean]("needsAuditing").getOrElse(true)
    ControllerConfig(logging, auditing)
  }
}

case class ControllerConfigs(private val controllers: Map[String, ControllerConfig]) {

  def get(controllerName: String): ControllerConfig =
    controllers.getOrElse(controllerName, ControllerConfig())

  def controllerNeedsAuditing(controllerName: String): Boolean =
    get(controllerName).auditing
}

object ControllerConfigs {

  def fromConfig(configuration: Configuration): ControllerConfigs = {

    val configMap = (
      for (config             <- configuration.getOptional[Configuration]("controllers").toSeq;
           controllerName     <- controllerNames(config);
           entryForController <- readCompositeValue(config, controllerName);
           parsedEntryForController = ControllerConfig.fromConfig(entryForController))
        yield (controllerName, parsedEntryForController)
    ).toMap

    ControllerConfigs(configMap)
  }

  def controllerNames(config: Configuration): List[String] = {
    def keepOnlyControllerName(s: String): String = {
      val lastChar = if (s.lastIndexOf(".") != -1) s.lastIndexOf(".") else s.length
      s.substring(0, lastChar)
    }
    config.keys.map(keepOnlyControllerName).toList
  }

  private def readCompositeValue(configuration: Configuration, key: String): Option[Configuration] =
    if (configuration.underlying.hasPathOrNull(key)) {
      configuration.underlying.getValue(key) match {
        case o: ConfigObject => Some(Configuration(o.toConfig))
        case _               => None
      }
    } else None
}
