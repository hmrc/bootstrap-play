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
import play.api.mvc.{AnyContent, MultipartFormData, Request}

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

// Based on play.api.data.DefaultFormBinding - removing the ability to bind forms from JSON
class UrlEncodedAndMultipartFormBinding extends FormBinding {
  override def apply(request: play.api.mvc.Request[_]): Map[String, Seq[String]] = {
    import play.api.mvc.MultipartFormData
    val unwrap = request.body match {
      case body: play.api.mvc.AnyContent =>
        body.asFormUrlEncoded.orElse(body.asMultipartFormData).getOrElse(body)
      case body => body
    }
    val data: Map[String, Seq[String]] = unwrap match {
      case body: Map[_, _]                   => body.asInstanceOf[Map[String, Seq[String]]]
      case body: MultipartFormData[_]        => multipartFormParse(body)
      case Right(body: MultipartFormData[_]) => multipartFormParse(body)
      case _                                 => Map.empty
    }
    (data ++ QueryStringMapper(request)).toMap
  }
  private def multipartFormParse(body: MultipartFormData[_]) = body.asFormUrlEncoded
}

// helper
private object QueryStringMapper {
  def apply(request: play.api.mvc.Request[_]): Map[_ <: String, Seq[String]] = request.method.toUpperCase match {
    case HttpVerbs.POST | HttpVerbs.PUT | HttpVerbs.PATCH => Map.empty
    case _                                                => request.queryString
  }
}

