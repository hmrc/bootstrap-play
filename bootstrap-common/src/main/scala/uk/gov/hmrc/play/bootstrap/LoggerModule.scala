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
       reqLogger <- config.keys
       reqLevel  =  config.get[String](reqLogger)
       level     =  { val level = Level.toLevel(reqLevel, null)
                      if (level == null) throw new IllegalArgumentException(s"Cannot set logger '$reqLogger'. '$reqLevel' is not a valid log level")
                      level
                    }
     } yield (reqLogger, level)
    ).foreach { case (reqLogger, level) =>
      logger.info(s"Configuring logger $reqLogger to level $level")
      LoggerFactory.getLogger(reqLogger).asInstanceOf[Logger].setLevel(level)
    }

    Seq.empty
  }
}
