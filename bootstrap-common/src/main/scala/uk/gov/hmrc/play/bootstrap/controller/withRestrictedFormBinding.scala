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

package uk.gov.hmrc.play.bootstrap.controller

import play.api.data.{DefaultFormBinding, FormBinding}
import play.api.mvc.BaseControllerHelpers
import uk.gov.hmrc.play.bootstrap.data.{UrlEncodedAndMultipartFormBinding, UrlEncodedOnlyFormBinding}

trait WithUrlEncodedAndMultipartFormBinding { self: BaseControllerHelpers =>

  override implicit lazy val defaultFormBinding: FormBinding = new UrlEncodedAndMultipartFormBinding
}

trait WithUrlEncodedOnlyFormBinding { self: BaseControllerHelpers =>

  override implicit lazy val defaultFormBinding: FormBinding = new UrlEncodedOnlyFormBinding
}

trait WithUnsafeDefaultFormBinding { self: BaseControllerHelpers =>

  override implicit lazy val defaultFormBinding: FormBinding = new DefaultFormBinding(parse.DefaultMaxTextLength)
}
