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

import play.api.Logger
import play.api.data.FormBinding
import play.api.http.HttpVerbs
import play.api.mvc.{AnyContent, MultipartFormData, Request}

// Based on play.api.data.DefaultFormBinding
abstract class RestrictedFormBinding extends FormBinding {

  private val logger = Logger(getClass)

  protected def matchBody(body: Any): Map[String, Seq[String]]

  override def apply(request: play.api.mvc.Request[_]): Map[String, Seq[String]] = {
    request.method.toUpperCase match {
      case HttpVerbs.POST | HttpVerbs.PUT | HttpVerbs.PATCH => parseBody(request)
      case _                                                => request.queryString
    }
  }

  private def parseBody(request: Request[_]) = {
    val unwrap = request.body match {
      case body: AnyContent =>
        body.asFormUrlEncoded.orElse(body.asMultipartFormData).getOrElse(body)
      case body => body
    }
    matchBody(unwrap)
  }

  protected def noBody(): Map[String, Seq[String]] = {
    logger.warn(
      "Could not find a body for form binding - possibly an invalid content type for this controller - " +
      "if you are parsing multipart requests please use 'with WithUrlEncodedAndMultipartFormBinding' on your controller")
    Map.empty
  }
}

// Based on play.api.data.DefaultFormBinding - removing the ability to bind forms from JSON and multipart data
class UrlEncodedOnlyFormBinding extends RestrictedFormBinding {

  override protected def matchBody(unwrap: Any): Map[String, Seq[String]] = unwrap match {
    case body: Map[_, _]                   => body.asInstanceOf[Map[String, Seq[String]]]
    case _                                 => noBody()
  }
}

// Based on play.api.data.DefaultFormBinding - removing the ability to bind forms from JSON
class UrlEncodedAndMultipartFormBinding extends RestrictedFormBinding {

  override protected def matchBody(unwrap: Any): Map[String, Seq[String]] = unwrap match {
    case body: Map[_, _]                   => body.asInstanceOf[Map[String, Seq[String]]]
    case body: MultipartFormData[_]        => multipartFormParse(body)
    case Right(body: MultipartFormData[_]) => multipartFormParse(body)
    case _                                 => noBody()
  }

  private def multipartFormParse(body: MultipartFormData[_]) = body.asFormUrlEncoded
}


