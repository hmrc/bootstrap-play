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

package uk.gov.hmrc.play.bootstrap

import ch.qos.logback.classic.{Level, Logger}
import org.slf4j.LoggerFactory
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}

class LoggerModule extends Module {

  private val logger = LoggerFactory.getLogger(getClass)

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    (for {
       config    <- configuration.getOptional[Configuration]("logger").toSet[Configuration]
       (k, v)    <- config.entrySet.toMap
                      .filterKeys(_ != "resource") // logger.resource configures logback location, not a level
                      .mapValues(_.unwrapped)
                      .map {
                        case (k, v: String) => k -> Option(Level.toLevel(v, null))
                        case (k, _        ) => k -> None
                      }
     } yield (k, v)
    ).foreach {
      case (reqLogger, Some(level)) => logger.info(s"Configuring logger $reqLogger to level $level")
                                       LoggerFactory.getLogger(reqLogger).asInstanceOf[Logger].setLevel(level)
      case (reqLogger, None       ) => logger.warn(s"Skipping 'logger.$reqLogger' - not a valid log level")
    }

    Seq.empty
  }
}
