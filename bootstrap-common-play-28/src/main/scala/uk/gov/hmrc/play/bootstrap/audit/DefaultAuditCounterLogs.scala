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

package uk.gov.hmrc.play.bootstrap.audit

import ch.qos.logback.classic.{Level, Logger}
import org.slf4j.LoggerFactory
import uk.gov.hmrc.play.audit.http.connector.AuditCounterLogs

class DefaultAuditCounterLogs extends AuditCounterLogs {

  private val logger = LoggerFactory.getLogger("play-auditing")
  logger.asInstanceOf[Logger].setLevel(Level.INFO)

  override def logInfo(message: String): Unit = {
    logger.info(message)
  }

}
