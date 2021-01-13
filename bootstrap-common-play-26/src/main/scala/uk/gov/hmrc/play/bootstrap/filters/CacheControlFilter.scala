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

package uk.gov.hmrc.play.bootstrap.filters

import javax.inject.Inject

import akka.stream.Materializer
import play.api.{Configuration, Logger}
import play.api.http.HeaderNames.CACHE_CONTROL
import play.api.http.Status
import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future}

case class CacheControlConfig(cacheableContentTypes: Seq[String] = Seq.empty)

object CacheControlConfig {

  def fromConfig(configuration: Configuration): CacheControlConfig = {

    val cacheableContentTypes: Seq[String] =
      configuration.getOptional[Seq[String]]("caching.allowedContentTypes").getOrElse(Seq.empty)

    CacheControlConfig(cacheableContentTypes)
  }
}

class CacheControlFilter @Inject()(
  config: CacheControlConfig,
  override val mat: Materializer
)(implicit ec: ExecutionContext)
    extends Filter {

  private val logger = Logger(getClass)

  if (config.cacheableContentTypes.nonEmpty) {
    logger.info(s"Will allow caching of content types starting with: ${config.cacheableContentTypes.mkString(", ")}")
  }

  override def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] =
    f(rh).map { result =>
      def status = result.header.status

      def existingHeader = result.header.headers.get(CACHE_CONTROL)

      def contentTypeIsCacheable =
        result.body.contentType.exists { contentType =>
          config.cacheableContentTypes.exists(contentType.startsWith)
        }

      def headerShouldBeSet =
        !(status == Status.NOT_MODIFIED || existingHeader.isDefined || contentTypeIsCacheable)

      if (headerShouldBeSet) {
        result.withHeaders(CACHE_CONTROL -> CacheControlFilter.headerValue)
      } else {
        result
      }
    }
}

object CacheControlFilter {

  val headerValue: String = "no-cache,no-store,max-age=0"
}
