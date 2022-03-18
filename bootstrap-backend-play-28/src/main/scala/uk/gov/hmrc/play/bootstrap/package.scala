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

package uk.gov.hmrc.play

import akka.stream.Materializer
import com.github.ghik.silencer.silent
import com.kenshoo.play.metrics.MetricsFilter
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.{ControllerConfigs, HttpAuditEvent}

import scala.concurrent.ExecutionContext

package bootstrap {

  @deprecated("Use uk.gov.hmrc.play.bootstrap.backend.BackendModule instead", "2.12.0")
  class MicroserviceModule extends uk.gov.hmrc.play.bootstrap.backend.BackendModule

  package object controller {
    @deprecated("Use uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController instead", "2.12.0")
    type BackendBaseController = uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController

    @deprecated("Use uk.gov.hmrc.play.bootstrap.backend.controller.BackendController instead", "2.12.0")
    type BackendController = uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

    @deprecated("Use uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider instead", "2.12.0")
    type BackendHeaderCarrierProvider = uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider
  }

  package filters {
    @deprecated("Config setting play.http.filters = \"uk.gov.hmrc.play.bootstrap.backend.filters.BackendFilters\" is no longer required. Bootstrap filters are now configured via backend.conf", "4.0.0")
    @Singleton
    class MicroserviceFilters @Inject()(
      metricsFilter: MetricsFilter,
      auditFilter  : AuditFilter,
      loggingFilter: LoggingFilter,
      cacheFilter  : CacheControlFilter,
      mdcFilter    : MDCFilter
    ) extends uk.gov.hmrc.play.bootstrap.backend.filters.BackendFilters(
      metricsFilter,
      auditFilter,
      loggingFilter,
      cacheFilter,
      mdcFilter
    )

    package object microservice {
      @deprecated("Use uk.gov.hmrc.play.bootstrap.backend.filters.BackendAuditFilter instead", "2.12.0")
      type MicroserviceAuditFilter = uk.gov.hmrc.play.bootstrap.backend.filters.BackendAuditFilter
    }

    package microservice {
      @deprecated("Use uk.gov.hmrc.play.bootstrap.backend.filters.DefaultBackendAuditFilter instead", "2.12.0")
      class DefaultMicroserviceAuditFilter @Inject()(
        config           : Configuration,
        controllerConfigs: ControllerConfigs,
        auditConnector   : AuditConnector,
        httpAuditEvent   : HttpAuditEvent,
        mat              : Materializer
      )(implicit ec: ExecutionContext
      ) extends uk.gov.hmrc.play.bootstrap.backend.filters.DefaultBackendAuditFilter(
        config,
        controllerConfigs,
        auditConnector,
        httpAuditEvent,
        mat
      )
    }
  }

  package object http {
    @deprecated("Use uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse instead", "2.12.0")
    type ErrorResponse = uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

    @deprecated("Use uk.gov.hmrc.http.HttpClient instead", "2.15.0")
    type HttpClient = uk.gov.hmrc.http.HttpClient
  }

  package http {
    @deprecated("Use uk.gov.hmrc.play.bootstrap.backend.http.JsonErrorHandler instead", "2.12.0")
    class JsonErrorHandler @Inject()(
      auditConnector: AuditConnector,
      httpAuditEvent: HttpAuditEvent,
      configuration : Configuration
    )(implicit ec: ExecutionContext
    ) extends uk.gov.hmrc.play.bootstrap.backend.http.JsonErrorHandler(
      auditConnector: AuditConnector,
      httpAuditEvent: HttpAuditEvent,
      configuration : Configuration
    )
  }

  package object backend {
    @silent("deprecated")
    val deprecatedClasses: Map[String, String] =
      Map(
        classOf[uk.gov.hmrc.play.bootstrap.MicroserviceModule].getName                                  -> classOf[uk.gov.hmrc.play.bootstrap.backend.BackendModule].getName,
        classOf[uk.gov.hmrc.play.bootstrap.controller.BackendBaseController].getName                    -> classOf[uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController].getName,
        classOf[uk.gov.hmrc.play.bootstrap.controller.BackendController].getName                        -> classOf[uk.gov.hmrc.play.bootstrap.backend.controller.BackendController].getName,
        classOf[uk.gov.hmrc.play.bootstrap.controller.BackendHeaderCarrierProvider].getName             -> classOf[uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider].getName,
        classOf[uk.gov.hmrc.play.bootstrap.filters.MicroserviceFilters].getName                         -> classOf[uk.gov.hmrc.play.bootstrap.backend.filters.BackendFilters].getName,
        classOf[uk.gov.hmrc.play.bootstrap.filters.microservice.MicroserviceAuditFilter].getName        -> classOf[uk.gov.hmrc.play.bootstrap.backend.filters.BackendAuditFilter].getName,
        classOf[uk.gov.hmrc.play.bootstrap.filters.microservice.DefaultMicroserviceAuditFilter].getName -> classOf[uk.gov.hmrc.play.bootstrap.backend.filters.DefaultBackendAuditFilter].getName,
        classOf[uk.gov.hmrc.play.bootstrap.http.ErrorResponse].getName                                  -> classOf[uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse].getName,
        classOf[uk.gov.hmrc.play.bootstrap.http.JsonErrorHandler].getName                               -> classOf[uk.gov.hmrc.play.bootstrap.backend.http.JsonErrorHandler].getName
      )
  }
}
