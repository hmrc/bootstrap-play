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

package uk.gov.hmrc.play.bootstrap.http

import akka.actor.ActorSystem
import javax.inject.{Inject, Named, Singleton}
import play.api.Configuration
import play.api.libs.ws.WSClient
import uk.gov.hmrc.http.HttpClientImpl
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.util.matching.Regex

@Singleton
class DefaultHttpClient @Inject()(
  config      : Configuration,
  httpAuditing: HttpAuditing,
  wsClient    : WSClient,
  actorSystem : ActorSystem
) extends HttpClientImpl(
  configuration = config.underlying,
  hooks         = Seq(httpAuditing.AuditingHook),
  wsClient,
  actorSystem
)

@Singleton
class DefaultHttpAuditing @Inject() (
  override val auditConnector: AuditConnector,
  @Named("appName") override val appName: String,
  config: Configuration
) extends HttpAuditing {
  override def auditDisabledForPattern: Regex =
    config.get[String]("httpclient.audit.disabledFor").r
}
