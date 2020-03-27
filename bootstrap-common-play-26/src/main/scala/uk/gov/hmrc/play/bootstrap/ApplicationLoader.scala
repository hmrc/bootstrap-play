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

package uk.gov.hmrc.play.bootstrap

import play.api.ApplicationLoader.Context
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceApplicationLoader}
import uk.gov.hmrc.play.bootstrap.config.Base64ConfigDecoder

class ApplicationLoader extends GuiceApplicationLoader with Base64ConfigDecoder {

  override def builder(context: Context): GuiceApplicationBuilder =
    super.builder(context.copy(initialConfiguration = decodeConfig(context.initialConfiguration)))
}
