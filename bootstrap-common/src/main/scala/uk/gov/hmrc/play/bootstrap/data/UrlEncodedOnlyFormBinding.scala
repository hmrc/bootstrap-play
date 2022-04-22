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

package uk.gov.hmrc.play.bootstrap.data

import play.api.data.FormBinding
import play.api.http.HttpVerbs
import play.api.mvc.{AnyContent, Request}

// Based on play.api.data.DefaultFormBinding - removing the ability to bind forms from JSON and multipart data
class UrlEncodedOnlyFormBinding extends FormBinding {
  override def apply(request: Request[_]): Map[String, Seq[String]] = {
    val data = request.body match {
      case any: AnyContent => any.asFormUrlEncoded.getOrElse(Map.empty)
      case _               => Map.empty
    }
    (data ++ QueryStringMapper(request)).toMap
  }
}
