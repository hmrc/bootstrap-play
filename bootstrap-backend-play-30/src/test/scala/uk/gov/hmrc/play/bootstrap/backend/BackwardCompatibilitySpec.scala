/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.play.bootstrap.backend

import com.typesafe.config.ConfigFactory
import org.apache.pekko.stream.Materializer
import org.mockito.scalatest.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.Configuration
import play.api.libs.json.{JsObject, OFormat}
import play.api.mvc.{ControllerComponents, RequestHeader}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.{ExtendedDataEvent, RedactionLog, TruncationLog}

import scala.concurrent.ExecutionContext

@annotation.nowarn("msg=deprecated")
class BackwardCompatibilitySpec
  extends AnyWordSpecLike
     with Matchers
     with MockitoSugar {

  val environment = play.api.Environment.simple()

  "package" should {
    "preserve uk.gov.hmrc.play.bootstrap.MicroserviceModule" in {
      new uk.gov.hmrc.play.bootstrap.MicroserviceModule

      // and can load from config
      environment.classLoader.loadClass("uk.gov.hmrc.play.bootstrap.MicroserviceModule")
    }

    "preserve uk.gov.hmrc.play.bootstrap.controller.BackendBaseController" in {
      new uk.gov.hmrc.play.bootstrap.controller.BackendBaseController {
        override val controllerComponents = mock[ControllerComponents]
      }
    }

    "preserve uk.gov.hmrc.play.bootstrap.controller.BackendController" in {
      new uk.gov.hmrc.play.bootstrap.controller.BackendController(
        controllerComponents = mock[ControllerComponents]
      ) {}
    }

    "preserve uk.gov.hmrc.play.bootstrap.controller.BackendHeaderCarrierProvider" in {
      new uk.gov.hmrc.play.bootstrap.controller.BackendHeaderCarrierProvider {}
    }

    "preserve uk.gov.hmrc.play.bootstrap.filters.MicroserviceFilters" in {
      new uk.gov.hmrc.play.bootstrap.filters.MicroserviceFilters(
        metricsFilter = mock[com.kenshoo.play.metrics.MetricsFilter],
        auditFilter   = mock[uk.gov.hmrc.play.bootstrap.filters.AuditFilter],
        loggingFilter = mock[uk.gov.hmrc.play.bootstrap.filters.LoggingFilter],
        cacheFilter   = mock[uk.gov.hmrc.play.bootstrap.filters.CacheControlFilter],
        mdcFilter     = mock[uk.gov.hmrc.play.bootstrap.filters.MDCFilter]
      )

      // and can load from config
      environment.classLoader.loadClass("uk.gov.hmrc.play.bootstrap.filters.MicroserviceFilters")
    }

    "preserve uk.gov.hmrc.play.bootstrap.filters.microservice.MicroserviceAuditFilter" in {
      new uk.gov.hmrc.play.bootstrap.filters.microservice.MicroserviceAuditFilter {
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
          truncationLog  : TruncationLog,
          redactionLog   : RedactionLog
        )(implicit hc: HeaderCarrier): ExtendedDataEvent = mock[ExtendedDataEvent]
      }
    }

    "preserve uk.gov.hmrc.play.bootstrap.filters.microservice.DefaultMicroserviceAuditFilter" in {
      new uk.gov.hmrc.play.bootstrap.filters.microservice.DefaultMicroserviceAuditFilter(
        config            = Configuration(ConfigFactory.load()),
        controllerConfigs = mock[uk.gov.hmrc.play.bootstrap.config.ControllerConfigs],
        auditConnector    = mock[uk.gov.hmrc.play.audit.http.connector.AuditConnector],
        httpAuditEvent    = mock[uk.gov.hmrc.play.bootstrap.config.HttpAuditEvent],
        mat               = mock[Materializer]
      )(ec                = mock[ExecutionContext])
    }

    "preserve uk.gov.hmrc.play.bootstrap.http.JsonErrorHandler" in {
      new uk.gov.hmrc.play.bootstrap.http.JsonErrorHandler(
        auditConnector = mock[uk.gov.hmrc.play.audit.http.connector.AuditConnector],
        httpAuditEvent = mock[uk.gov.hmrc.play.bootstrap.config.HttpAuditEvent],
        configuration  = Configuration(ConfigFactory.load("backend.test.conf"))
      )(ec             = mock[ExecutionContext])

      // and can load from config
      environment.classLoader.loadClass("uk.gov.hmrc.play.bootstrap.http.JsonErrorHandler")
    }

    "preserve uk.gov.hmrc.play.bootstrap.http.ErrorResponse" in {
      new uk.gov.hmrc.play.bootstrap.http.ErrorResponse(
        statusCode  = 0,
        message     = "",
        xStatusCode = mock[Option[String]],
        requested   = mock[Option[String]]
      )
    }

    "preserve implicit OFormat[uk.gov.hmrc.play.bootstrap.http.ErrorResponse]" in {
      implicitly[OFormat[uk.gov.hmrc.play.bootstrap.http.ErrorResponse]]
    }
  }
}
