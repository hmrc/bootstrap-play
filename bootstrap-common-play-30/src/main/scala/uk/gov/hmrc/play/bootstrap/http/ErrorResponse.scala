/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{OFormat, __}

/**
  * @param statusCode  the http status code
  * @param message     error message
  * @param xStatusCode error code
  * @param requested   the requested URL
  */
case class ErrorResponse(
  statusCode : Int
, message    : String
, xStatusCode: Option[String] = None
, requested  : Option[String] = None
)

object ErrorResponse {
  implicit val format: OFormat[ErrorResponse] =
    ( (__ \ "statusCode" ).format[Int]
    ~ (__ \ "message"    ).format[String]
    ~ (__ \ "xStatusCode").formatNullable[String]
    ~ (__ \ "requested"  ).formatNullable[String]
    )(ErrorResponse.apply, er => (er.statusCode, er.message, er.xStatusCode, er.requested))
}
