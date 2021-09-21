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
import javax.inject.{Inject, Named, Singleton}
import play.api.Configuration
import play.api.libs.ws.{DefaultWSProxyServer, WSClient, WSProxyServer}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.http.ws._

import scala.util.matching.Regex

@Singleton
class DefaultHttpClient @Inject()(
  config: Configuration,
  val httpAuditing: HttpAuditing,
  override val wsClient: WSClient,
  override protected val actorSystem: ActorSystem
) extends HttpClient
     with WSHttp {

  override lazy val configuration: Config = config.underlying

  override val hooks: Seq[HttpHook] = Seq(httpAuditing.AuditingHook)

  override def withUserAgent(userAgent: String): DefaultHttpClient =
    new DefaultHttpClient(
      config = Configuration("appName" -> userAgent) ++ config,
      httpAuditing,
      wsClient,
      actorSystem
    )

  override def withProxy(): DefaultHttpClient =
    new DefaultHttpClient(
      config,
      httpAuditing,
      wsClient,
      actorSystem
    ) {
      override val wsProxyServer: Option[WSProxyServer] = {
        if (config.get[Boolean]("proxy.proxyRequiredForThisEnvironment")) { // keep this check to avoid needing the following configuration for development? rename to `proxy.enabled`?
          import scala.collection.JavaConverters.iterableAsScalaIterableConverter
          Some(
            DefaultWSProxyServer(
              protocol  = Some(config.get[String]("proxy.protocol")),
              host      = config.get[String]("proxy.host"),
              port      = config.get[Int]("proxy.port"),
              principal = config.getOptional[String]("proxy.username"),
              password  = config.getOptional[String]("proxy.password"),
              // following exists to be development friendly (necessary with `  proxy.proxyRequiredForThisEnvironment`?)
              nonProxyHosts = Some(Seq("localhost"))
              //nonProxyHosts = Some(configuration.underlying.getStringList("proxy.nonProxyHosts").asScala.toSeq)
            )
          )
        } else None
      }
    }
}

@Singleton
class DefaultHttpAuditing @Inject() (
  val auditConnector: AuditConnector,
  @Named("appName") val appName: String,
  config: Configuration
) extends HttpAuditing {
  override def auditDisabledForPattern: Regex =
    config.get[String]("httpclient.audit.disabledFor").r
}
