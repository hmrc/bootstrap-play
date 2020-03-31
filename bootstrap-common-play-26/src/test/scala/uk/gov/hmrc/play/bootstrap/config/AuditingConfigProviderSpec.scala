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

import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import uk.gov.hmrc.play.audit.http.config.{AuditingConfig, BaseUri, Consumer}

class AuditingConfigProviderSpec extends AnyWordSpec with Matchers with MockitoSugar {

  private val appName       = "app-name"
  private val mockedRunMode = mock[RunMode]
  when(mockedRunMode.env).thenReturn("Test")

  "LoadAuditingConfig" should {

    "use env specific settings if these provided" in {
      val config = Configuration(
        "Test.auditing.enabled"               -> "true",
        "Test.auditing.traceRequests"         -> "true",
        "Test.auditing.consumer.baseUri.host" -> "localhost",
        "Test.auditing.consumer.baseUri.port" -> "8100",
        "auditing.enabled"                    -> "false",
        "auditing.consumer.baseUri.host"      -> "foo",
        "auditing.consumer.baseUri.port"      -> "1234"
      )

      new AuditingConfigProvider(config, mockedRunMode, appName).get() shouldBe AuditingConfig(
        consumer    = Some(Consumer(BaseUri("localhost", 8100, "http"))),
        enabled     = true,
        auditSource = appName
      )
    }

    "fallback to non-env specific config" in {
      val configuration = Configuration(
        "auditing.enabled"               -> "true",
        "auditing.traceRequests"         -> "true",
        "auditing.consumer.baseUri.host" -> "localhost",
        "auditing.consumer.baseUri.port" -> "8100"
      )

      new AuditingConfigProvider(configuration, mockedRunMode, appName).get() shouldBe AuditingConfig(
        consumer    = Some(Consumer(BaseUri("localhost", 8100, "http"))),
        enabled     = true,
        auditSource = appName
      )
    }

    "allow audit to be disabled" in {
      val config = Configuration(
        "auditing.enabled" -> "false"
      )

      new AuditingConfigProvider(config, mockedRunMode, appName).get() shouldBe AuditingConfig(
        consumer    = None,
        enabled     = false,
        auditSource = "auditing disabled"
      )
    }
  }
}
