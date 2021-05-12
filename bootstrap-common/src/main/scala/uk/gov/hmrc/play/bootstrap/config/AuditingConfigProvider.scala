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

package uk.gov.hmrc.play.bootstrap.config

import javax.inject.{Inject, Named, Provider}
import play.api.Configuration
import uk.gov.hmrc.play.audit.http.config.{AuditingConfig, BaseUri, Consumer}

class AuditingConfigProvider @Inject()(
  configuration: Configuration,
  @Named("appName") appName: String
) extends Provider[AuditingConfig] {

  def get(): AuditingConfig = {
    val c = configuration.get[Configuration]("auditing")
    val enabled = c.get[Boolean]("enabled")

    if (enabled) {
      val con = getRequired[Configuration](c, "consumer", "Missing consumer configuration `auditing.consumer` for auditing")
      val uri = getRequired[Configuration](con, "baseUri", "Missing consumer baseUri `auditing.consumer.baseUri` for auditing")
      AuditingConfig(
        enabled           = enabled,
        consumer          = Some(
                              Consumer(
                                BaseUri(
                                  host     = getRequired[String](uri, "host", "Missing consumer host `auditing.consumer.baseUri.host` for auditing"),
                                  port     = getRequired[Int](uri, "port", "Missing consumer port `auditing.consumer.baseUri.port` for auditing"),
                                  protocol = uri.getOptional[String]("protocol").getOrElse("http")
                                )
                              )
                            ),
        auditSource       = appName,
        auditSentHeaders  = c.get[Boolean]("auditSentHeaders"),
        publishCountersToLogs = c.getOptional[Boolean]("publishCountersToLogs").getOrElse(true)
      )
    } else
      AuditingConfig(consumer = None, enabled = false, auditSource = "auditing disabled", auditSentHeaders = false, publishCountersToLogs = false)
  }

  private def getRequired[T: play.api.ConfigLoader](config: Configuration, key: String, errMsg: => String): T =
    config.getOptional[T](key).getOrElse(sys.error(errMsg))
}
