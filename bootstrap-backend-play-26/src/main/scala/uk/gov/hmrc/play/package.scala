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

package uk.gov.hmrc.play

import akka.stream.Materializer
import com.kenshoo.play.metrics.MetricsFilter
import javax.inject.{Inject, Singleton}
import play.api.http.DefaultHttpFilters
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.filters.{AuditFilter, CacheControlFilter, LoggingFilter, MDCFilter}
import uk.gov.hmrc.play.bootstrap.config.{ControllerConfigs, HttpAuditEvent}

import scala.concurrent.ExecutionContext

package bootstrap {

  package object controller {
    @deprecated("Use uk.gov.hmrc.play.bootstrap.controller.backend.BackendController", "2.6.0")
    type BackendController = uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
  }

  package filters {
    @deprecated("Use uk.gov.hmrc.play.bootstrap.backend.filters.MicroserviceFilters", "2.6.0")
    @Singleton
    class MicroserviceFilters @Inject()(
      metricsFilter: MetricsFilter,
      auditFilter  : AuditFilter,
      loggingFilter: LoggingFilter,
      cacheFilter  : CacheControlFilter,
      mdcFilter    : MDCFilter
    ) extends DefaultHttpFilters(
      metricsFilter,
      auditFilter,
      loggingFilter,
      cacheFilter,
      mdcFilter
    )

    package object microservice {
      @deprecated("Use uk.gov.hmrc.play.bootstrap.backend.filters.MicroserviceAuditFilter", "2.6.0")
      type MicroserviceAuditFilter = uk.gov.hmrc.play.bootstrap.backend.filters.MicroserviceAuditFilter
    }

    package microservice {
      @deprecated("Use uk.gov.hmrc.play.bootstrap.backend.filters.DefaultMicroserviceAuditFilter instead", "2.6.0")
      class DefaultMicroserviceAuditFilter @Inject()(
        controllerConfigs: ControllerConfigs,
        auditConnector   : AuditConnector,
        httpAuditEvent   : HttpAuditEvent,
        mat              : Materializer
      )(implicit ec: ExecutionContext
      ) extends uk.gov.hmrc.play.bootstrap.backend.filters.DefaultMicroserviceAuditFilter(
        controllerConfigs,
        auditConnector,
        httpAuditEvent,
        mat
      )
    }
  }
}