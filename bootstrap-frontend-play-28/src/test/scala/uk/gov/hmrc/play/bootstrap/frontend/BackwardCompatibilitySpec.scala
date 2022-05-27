/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.play.bootstrap.frontend

import akka.stream.Materializer
import com.github.ghik.silencer.silent
import com.typesafe.config.ConfigFactory
import org.mockito.scalatest.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.libs.json.JsObject
import play.api.mvc.{MessagesControllerComponents, RequestHeader}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.{ExtendedDataEvent, TruncationLog}
import uk.gov.hmrc.play.bootstrap.frontend.filters.AuditableRequestHeaders

import scala.concurrent.ExecutionContext

@silent("deprecated")
class BackwardCompatibilitySpec
  extends AnyWordSpecLike
     with Matchers
     with MockitoSugar {

  val environment = play.api.Environment.simple()

  "pacakge" should {
    "preserve uk.gov.hmrc.play.bootstrap.FrontendModule" in {
      new uk.gov.hmrc.play.bootstrap.FrontendModule

      // and can load from config
      environment.classLoader.loadClass("uk.gov.hmrc.play.bootstrap.FrontendModule")
    }

    "preserve uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController" in {
      new uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController {
        override val controllerComponents = mock[MessagesControllerComponents]
      }
    }

    "preserve uk.gov.hmrc.play.bootstrap.controller.FrontendController" in {
      new uk.gov.hmrc.play.bootstrap.controller.FrontendController(
        controllerComponents = mock[MessagesControllerComponents]
      ) {}
    }

    "preserve uk.gov.hmrc.play.bootstrap.controller.FrontendHeaderCarrierProvider" in {
      new uk.gov.hmrc.play.bootstrap.controller.FrontendHeaderCarrierProvider {}
    }

    "preserve uk.gov.hmrc.play.bootstrap.filters.FrontendFilters" in {
      new uk.gov.hmrc.play.bootstrap.filters.FrontendFilters(
        configuration             = Configuration(ConfigFactory.load("frontend.conf")),
        loggingFilter             = mock[uk.gov.hmrc.play.bootstrap.filters.LoggingFilter],
        headersFilter             = mock[uk.gov.hmrc.play.bootstrap.filters.frontend.HeadersFilter],
        securityFilter            = mock[play.filters.headers.SecurityHeadersFilter],
        frontendAuditFilter       = mock[uk.gov.hmrc.play.bootstrap.filters.AuditFilter],
        metricsFilter             = mock[com.kenshoo.play.metrics.MetricsFilter],
        deviceIdFilter            = mock[uk.gov.hmrc.play.bootstrap.filters.frontend.deviceid.DeviceIdFilter],
        csrfFilter                = mock[play.filters.csrf.CSRFFilter],
        sessionCookieCryptoFilter = mock[uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoFilter],
        sessionTimeoutFilter      = mock[uk.gov.hmrc.play.bootstrap.filters.frontend.SessionTimeoutFilter],
        cacheControlFilter        = mock[uk.gov.hmrc.play.bootstrap.filters.CacheControlFilter],
        mdcFilter                 = mock[uk.gov.hmrc.play.bootstrap.filters.MDCFilter],
        allowlistFilter           = mock[uk.gov.hmrc.play.bootstrap.frontend.filters.AllowlistFilter],
        sessionIdFilter           = mock[uk.gov.hmrc.play.bootstrap.frontend.filters.SessionIdFilter]
      )

      // and can load from config
      environment.classLoader.loadClass("uk.gov.hmrc.play.bootstrap.filters.FrontendFilters")
    }

    "preserve uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.ApplicationCryptoProvider" in {
      new uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.ApplicationCryptoProvider(
        configuration = Configuration(ConfigFactory.load("frontend.conf"))
      )
    }

    "preserve uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCrypto" in {
      import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
      new uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCrypto(
        crypto = mock[Encrypter with Decrypter]
      )
    }

    "preserve uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoProvider" in {
      new uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoProvider(
        applicationCrypto = mock[uk.gov.hmrc.crypto.ApplicationCrypto]
      )
    }

    "preserve uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.CryptoImplicits" in {
      new uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.CryptoImplicits {}
    }

    "preserve uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoFilter" in {
      new uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCryptoFilter {
        override def ec                   = mock[ExecutionContext]
        override def mat                  = mock[Materializer]
        override def encrypter            = mock[uk.gov.hmrc.crypto.Encrypter]
        override def decrypter            = mock[uk.gov.hmrc.crypto.Decrypter]
        override def sessionBaker         = mock[play.api.mvc.SessionCookieBaker]
        override def cookieHeaderEncoding = mock[play.api.mvc.CookieHeaderEncoding]
      }
    }

    "preserve uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.DefaultSessionCookieCryptoFilter" in {
      new uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.DefaultSessionCookieCryptoFilter(
        sessionCookieCrypto  = mock[uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCrypto],
        sessionBaker         = mock[play.api.mvc.SessionCookieBaker],
        cookieHeaderEncoding = mock[play.api.mvc.CookieHeaderEncoding]
      )(mat                  = mock[Materializer],
        ec                   = mock[ExecutionContext]
      )
    }

    "preserve uk.gov.hmrc.play.bootstrap.filters.frontend.deviceid.DeviceFingerprint" in {
      new uk.gov.hmrc.play.bootstrap.filters.frontend.deviceid.DeviceFingerprint {}
      uk.gov.hmrc.play.bootstrap.filters.frontend.deviceid.DeviceFingerprint
    }

    "preserve uk.gov.hmrc.play.bootstrap.filters.frontend.deviceid.DeviceId" in {
      new uk.gov.hmrc.play.bootstrap.filters.frontend.deviceid.DeviceId(uuid = "", timestamp = 0L, hash = "")
      uk.gov.hmrc.play.bootstrap.filters.frontend.deviceid.DeviceId.Token1
    }

    "preserve uk.gov.hmrc.play.bootstrap.filters.frontend.deviceid.DeviceIdCookie" in {
      new uk.gov.hmrc.play.bootstrap.filters.frontend.deviceid.DeviceIdCookie {
        override val secret          = ""
        override val previousSecrets = Seq.empty
        override val secure          = true
      }
    }

    "preserve uk.gov.hmrc.play.bootstrap.filters.frontend.deviceid.DeviceIdFilter" in {
      new uk.gov.hmrc.play.bootstrap.filters.frontend.deviceid.DeviceIdFilter {
        override def ec              = mock[ExecutionContext]
        override def auditConnector  = mock[uk.gov.hmrc.play.audit.http.connector.AuditConnector]
        override def appName         = ""
        override val previousSecrets = mock[Seq[String]]
        override val secret          = ""
        override val secure          = true
        override def mat             = mock[Materializer]
      }
    }

    "preserve uk.gov.hmrc.play.bootstrap.filters.frontend.deviceid.DefaultDeviceIdFilter" in {
      new uk.gov.hmrc.play.bootstrap.filters.frontend.deviceid.DefaultDeviceIdFilter(
        appName        = "",
        configuration  = Configuration(),
        auditConnector = mock[uk.gov.hmrc.play.audit.http.connector.AuditConnector]
      )(mat            = mock[Materializer],
        ec             = mock[ExecutionContext]
      )
    }

    "preserve uk.gov.hmrc.play.bootstrap.filters.frontend.FrontendAuditFilter" in {
      new uk.gov.hmrc.play.bootstrap.filters.frontend.FrontendAuditFilter {
        override def ec             = mock[ExecutionContext]
        override def config         = Configuration(ConfigFactory.load())
        override def auditConnector = mock[uk.gov.hmrc.play.audit.http.connector.AuditConnector]
        override def mat            = mock[Materializer]
        override def controllerNeedsAuditing(controllerName: String): Boolean = true
        override def extendedDataEvent(
          eventType      : String,
          transactionName: String,
          request        : RequestHeader,
          detail         : JsObject,
          truncationLog  : Option[TruncationLog]
        )(implicit hc: HeaderCarrier): ExtendedDataEvent = mock[ExtendedDataEvent]
        override def maskedFormFields = mock[Seq[String]]
        override def applicationPort  = mock[Option[Int]]
        override def shouldAuditAllHeaders = false
        override def headerRedactions = AuditableRequestHeaders.Redactions.empty
      }
    }

    "preserve uk.gov.hmrc.play.bootstrap.filters.frontend.DefaultFrontendAuditFilter" in {
      new uk.gov.hmrc.play.bootstrap.filters.frontend.DefaultFrontendAuditFilter(
        config            = Configuration(ConfigFactory.load()),
        controllerConfigs = mock[uk.gov.hmrc.play.bootstrap.config.ControllerConfigs],
        auditConnector    = mock[uk.gov.hmrc.play.audit.http.connector.AuditConnector],
        httpAuditEvent    = mock[uk.gov.hmrc.play.bootstrap.config.HttpAuditEvent],
        mat               = mock[Materializer]
      )(ec                = mock[ExecutionContext])
    }

    "preserve uk.gov.hmrc.play.bootstrap.filters.frontend.HeadersFilter" in {
      new uk.gov.hmrc.play.bootstrap.filters.frontend.HeadersFilter(
        mat = mock[Materializer]
      )
    }

    "preserve uk.gov.hmrc.play.bootstrap.filters.frontend.SessionTimeoutFilterConfig" in {
      new uk.gov.hmrc.play.bootstrap.filters.frontend.SessionTimeoutFilterConfig(
        timeoutDuration       = java.time.Duration.ZERO, // Note this has been changed from joda.time.Duration to java.time.Duration, but all clients seem to call SessionTimeoutFilterConfig.fromConfig
        additionalSessionKeys = mock[Set[String]],
        onlyWipeAuthToken     = true
      )
      uk.gov.hmrc.play.bootstrap.filters.frontend.SessionTimeoutFilterConfig
    }

    "preserve uk.gov.hmrc.play.bootstrap.filters.frontend.SessionTimeoutFilter" in {
      new uk.gov.hmrc.play.bootstrap.filters.frontend.SessionTimeoutFilter(
        config = mock[uk.gov.hmrc.play.bootstrap.filters.frontend.SessionTimeoutFilterConfig]
      )(ec  = mock[ExecutionContext],
        mat = mock[Materializer]
      )
      uk.gov.hmrc.play.bootstrap.filters.frontend.SessionTimeoutFilter
    }

    "preserve uk.gov.hmrc.play.bootstrap.http.FrontendErrorHandler" in {
      new uk.gov.hmrc.play.bootstrap.http.FrontendErrorHandler {
        override def standardErrorTemplate(
          pageTitle: String,
          heading  : String,
          message  : String
          )(implicit request: play.api.mvc.Request[_]
          ) = mock [play.twirl.api.Html]
        override def messagesApi = mock[MessagesApi]
      }
    }

    "preserve uk.gov.hmrc.play.bootstrap.http.ApplicationException" in {
      new uk.gov.hmrc.play.bootstrap.http.ApplicationException(
        result  = mock[play.api.mvc.Result],
        message = ""
      )
    }
  }
}
