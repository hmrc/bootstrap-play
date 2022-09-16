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

package uk.gov.hmrc.play.bootstrap.frontend.filters

import javax.inject.Inject
import play.api.Logger
import play.api.http.HttpFilters

class FiltersVerifier @Inject() (
  filters: HttpFilters
) {
  private val logger = Logger(getClass)

  if (filters.filters.map(_.getClass.getSimpleName).filter(_ == "SessionIdFilter").size > 1)
    // or just warn if this is harmless
    logger.warn("Two SessionIdFilters have been enabled. If you are explicitly adding this filter, you can probably remove this and rely on the one provided by bootstrap-play.")
}
