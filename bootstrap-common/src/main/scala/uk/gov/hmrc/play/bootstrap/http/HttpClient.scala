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
import com.typesafe.config.Config
import javax.inject.{Inject, Named, Provider, Singleton}
import play.api.Configuration
import play.api.libs.ws.WSClient
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.http.HttpClientImpl
import uk.gov.hmrc.play.http.ws.WSHttp
import play.api.libs.ws.WSRequest

import scala.util.matching.Regex

// This implementation is not final, to not break clients. It deliberately does not override
// `withTransformRequest` since it would return an instance loosing any overrides.
// `uk.gov.hmrc.http.HttpClientImpl` should be used instead.
@deprecated("Use HttpClientProvider instead, which will provide an instance of `uk.gov.hmrc.http.HttpClientImpl", "13.9.0")
@Singleton
class DefaultHttpClient @Inject()(
  config      : Configuration,
  httpAuditing: HttpAuditing,
  override val wsClient    : WSClient,
  override val actorSystem : ActorSystem
) extends HttpClient with WSHttp {

  override val configuration: Config =
    config.underlying

  override val hooks: Seq[HttpHook] =
    Seq(httpAuditing.AuditingHook)
}

@Singleton
class HttpClientProvider @Inject()(
  config      : Configuration,
  httpAuditing: HttpAuditing,
  wsClient    : WSClient,
  actorSystem : ActorSystem
) extends Provider[HttpClient] {

  private lazy val instance = new HttpClientImpl(
    configuration = config.underlying,
    hooks         = Seq(httpAuditing.AuditingHook),
    wsClient,
    actorSystem,
    transformRequest = identity
  )

  override def get(): HttpClient =
    instance
}



@Singleton
class DefaultHttpAuditing @Inject() (
  override val auditConnector: AuditConnector,
  @Named("appName") override val appName: String,
  config: Configuration
) extends HttpAuditing {
  override def auditDisabledForPattern: Regex =
    config.get[String]("httpclient.audit.disabledFor").r
}
