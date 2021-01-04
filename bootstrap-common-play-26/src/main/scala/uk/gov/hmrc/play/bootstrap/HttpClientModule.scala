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

package uk.gov.hmrc.play.bootstrap

import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.CoreGet
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

class HttpClientModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = Seq(
    bind[HttpClient].to[DefaultHttpClient],
    // binding additional interfaces so that libraries that depend on http-verbs can be easily injected
    bind[CoreGet].to[DefaultHttpClient]
  )
}