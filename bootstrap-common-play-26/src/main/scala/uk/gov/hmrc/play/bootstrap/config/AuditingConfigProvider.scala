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

import javax.inject.{Inject, Named, Provider}
import play.api.Configuration
import uk.gov.hmrc.play.audit.http.config.{AuditingConfig, BaseUri, Consumer}

class AuditingConfigProvider @Inject()(
  configuration: Configuration,
  @Named("appName") appName: String
) extends Provider[AuditingConfig] {

  def get(): AuditingConfig = {
    configuration
      .getOptional[Configuration]("auditing")
      .map { c =>
        val enabled = c.get[Boolean]("enabled")

        if (enabled) {
          AuditingConfig(
            enabled = enabled,
            consumer = Some(
              c.getOptional[Configuration]("consumer")
                .map { con =>
                  Consumer(
                    baseUri = con
                      .getOptional[Configuration]("baseUri")
                      .map { uri =>
                        BaseUri(
                          host = uri
                            .getOptional[String]("host")
                            .getOrElse(throw new Exception("Missing consumer host for auditing")),
                          port = uri
                            .getOptional[Int]("port")
                            .getOrElse(throw new Exception("Missing consumer port for auditing")),
                          protocol = uri.getOptional[String]("protocol").getOrElse("http")
                        )
                      }
                      .getOrElse(throw new Exception("Missing consumer baseUri for auditing"))
                  )
                }
                .getOrElse(throw new Exception("Missing consumer configuration for auditing"))
            ),
            auditSource = appName,
            auditExtraHeaders = c.getOptional[Boolean]("auditExtraHeaders")
          )
        } else {
          AuditingConfig(consumer = None, enabled = false, auditSource = "auditing disabled")
        }
      }
  }.getOrElse(throw new Exception("Missing auditing configuration"))
}
