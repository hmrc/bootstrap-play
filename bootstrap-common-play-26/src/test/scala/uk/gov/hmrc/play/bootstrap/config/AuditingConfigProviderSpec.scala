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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import uk.gov.hmrc.play.audit.http.config.{AuditingConfig, BaseUri, Consumer}

class AuditingConfigProviderSpec extends AnyWordSpec with Matchers with MockitoSugar {

  private val appName = "app-name"

  "LoadAuditingConfig" should {

    "load config" in {
      val configuration = Configuration(
        "auditing.enabled"               -> "true",
        "auditing.traceRequests"         -> "true",
        "auditing.consumer.baseUri.host" -> "localhost",
        "auditing.consumer.baseUri.port" -> "8100"
      )

      new AuditingConfigProvider(configuration, appName).get() shouldBe AuditingConfig(
        consumer    = Some(Consumer(BaseUri("localhost", 8100, "http"))),
        enabled     = true,
        auditSource = appName
      )
    }

    "allow audit to be disabled" in {
      val config = Configuration(
        "auditing.enabled" -> "false"
      )

      new AuditingConfigProvider(config, appName).get() shouldBe AuditingConfig(
        consumer    = None,
        enabled     = false,
        auditSource = "auditing disabled"
      )
    }
  }
}
