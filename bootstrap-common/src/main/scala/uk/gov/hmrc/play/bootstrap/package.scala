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

package uk.gov.hmrc.play

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}

package bootstrap {

  package object binders {

    @deprecated("Use uk.gov.hmrc.play.bootstrap.binders.AbsoluteWithHostnameFromAllowlist instead", "4.0.0")
    object AbsoluteWithHostnameFromWhitelist {

      def apply(allowedHosts: String*): RedirectUrlPolicy[RedirectUrlPolicy.Id] =
        apply(allowedHosts.toSet)

      def apply(allowedHosts: Set[String]): RedirectUrlPolicy[RedirectUrlPolicy.Id] =
        AbsoluteWithHostnameFromAllowlist.apply(allowedHosts)

      def apply(allowedHostsFn: => Future[Set[String]])(implicit ec: ExecutionContext): RedirectUrlPolicy[Future] =
        AbsoluteWithHostnameFromAllowlist.apply(allowedHostsFn)
    }
  }

  @deprecated("Use uk.gov.hmrc.play.audit.AuditModule instead", "5.13.0")
  class AuditModule extends uk.gov.hmrc.play.audit.AuditModule // TODO or should Modules stay?

  package audit {
    @deprecated("Use uk.gov.hmrc.play.audit.DefaultAuditChannel instead", "5.13.0")
    @Singleton
    class DefaultAuditChannel @Inject()(
      auditingConfig   : uk.gov.hmrc.play.audit.http.config.AuditingConfig,
      materializer     : akka.stream.Materializer,
      lifecycle        : play.api.inject.ApplicationLifecycle,
      datastreamMetrics: uk.gov.hmrc.play.audit.http.connector.DatastreamMetrics
    ) extends uk.gov.hmrc.play.audit.DefaultAuditChannel(
      auditingConfig,
      materializer,
      lifecycle,
      datastreamMetrics
    )

    @deprecated("Use uk.gov.hmrc.play.audit.DefaultAuditChannel instead", "5.13.0")
    class DefaultAuditConnector @Inject()(
      auditingConfig   : uk.gov.hmrc.play.audit.http.config.AuditingConfig,
      auditChannel     : uk.gov.hmrc.play.audit.http.connector.AuditChannel,
      lifecycle        : play.api.inject.ApplicationLifecycle,
      datastreamMetrics: uk.gov.hmrc.play.audit.http.connector.DatastreamMetrics
    ) extends uk.gov.hmrc.play.audit.DefaultAuditConnector(
      auditingConfig,
      auditChannel,
      lifecycle,
      datastreamMetrics
    )

    // TODO move DatastreamMetricsProvider.scala into play-auditing
  }

  package config {
    @deprecated("Use uk.gov.hmrc.play.audit.http.config.AuditingConfigProvider instead", "5.13.0")
    class AuditingConfigProvider @Inject()(
      configuration: play.api.Configuration,
      @Named("appName") appName: String
    ) extends uk.gov.hmrc.play.audit.http.config.AuditingConfigProvider(
      configuration,
      appName
    )
  }
}

package object bootstrap {
  val deprecatedClasses: Map[String, String] =
    Map(
      classOf[uk.gov.hmrc.play.bootstrap.AuditModule].getName -> classOf[uk.gov.hmrc.play.audit.AuditModule].getName,
    )
}
