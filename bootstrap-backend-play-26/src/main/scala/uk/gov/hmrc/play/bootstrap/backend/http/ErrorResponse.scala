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

package uk.gov.hmrc.play.bootstrap.backend.http

import play.api.http.Status
import play.api.libs.json.{Json, OFormat, OWrites, Reads}

case class ErrorResponse(
  statusCode: Int,
  message: String,
  xStatusCode: Option[String] = None,
  requested: Option[String]   = None
) {
  val code: String = statusCode match {
    case clientError if Status.isClientError(clientError) =>
      "CLIENT_ERROR"
    case _ =>
      "SERVER_ERROR"
  }
}

object ErrorResponse {
  implicit val reads: Reads[ErrorResponse] =
    Json.reads[ErrorResponse]

  implicit val writes: OWrites[ErrorResponse] =
    OWrites.transform(Json.writes[ErrorResponse]) {
      case (err, jsObject) =>
        jsObject ++ Json.obj("code" -> err.code)
    }

  implicit val format: OFormat[ErrorResponse] =
    OFormat(reads, writes)
}
