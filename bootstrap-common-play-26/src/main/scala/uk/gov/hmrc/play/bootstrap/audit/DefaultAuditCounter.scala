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

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.play.audit.http.connector.AuditCounter

// audit counters are disable for play-26 because audit counters
// rely on CoordinatedShutdown to publish the final counters at the right time
// during shutdown and this class can not be used in play-26
@Singleton
class DefaultAuditCounter @Inject() extends AuditCounter {
  override def createMetadata(): JsObject = Json.obj()
}
