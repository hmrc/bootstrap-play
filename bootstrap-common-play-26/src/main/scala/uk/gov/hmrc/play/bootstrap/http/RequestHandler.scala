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

package uk.gov.hmrc.play.bootstrap.http

import javax.inject.Inject

import play.api.http.{DefaultHttpRequestHandler, HttpConfiguration, HttpErrorHandler, HttpFilters}
import play.api.mvc.{Handler, RequestHeader}
import play.api.routing.Router

class RequestHandler @Inject()(
  router: Router,
  errorHandler: HttpErrorHandler,
  configuration: HttpConfiguration,
  filters: HttpFilters)
    extends DefaultHttpRequestHandler(router, errorHandler, configuration, filters) {

  // Play 2.0 doesn't support trailing slash
  override def routeRequest(request: RequestHeader): Option[Handler] = super.routeRequest(request).orElse {
    if (request.path.endsWith("/")) {
      val pathWithoutSlash        = request.path.dropRight(1)
      val requestWithModifiedPath = request.withTarget(request.target.withPath(pathWithoutSlash))
      super.routeRequest(requestWithModifiedPath)
    } else {
      None
    }
  }
}
