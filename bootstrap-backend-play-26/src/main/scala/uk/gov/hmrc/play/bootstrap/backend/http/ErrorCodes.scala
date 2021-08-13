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

/** JSON response error codes described at [[https://developer.service.hmrc.gov.uk/api-documentation/docs/reference-guide#errors HMRC Developer Hub Reference Guide]]
  */
object ErrorCodes {
  val CLIENT_ERROR = "CLIENT_ERROR"

  val BAD_REQUEST                 = "BAD_REQUEST"
  val MISSING_CREDENTIALS         = "MISSING_CREDENTIALS"
  val INVALID_CREDENTIALS         = "INVALID_CREDENTIALS"
  val UNAUTHORIZED                = "UNAUTHORIZED"
  val INCORRECT_ACCESS_TOKEN_TYPE = "INCORRECT_ACCESS_TOKEN_TYPE"
  val HTTPS_REQUIRED              = "HTTPS_REQUIRED"
  val RESOURCE_FORBIDDEN          = "RESOURCE_FORBIDDEN"
  val INVALID_SCOPE               = "INVALID_SCOPE"
  val FORBIDDEN                   = "FORBIDDEN"
  val MATCHING_RESOURCE_NOT_FOUND = "MATCHING_RESOURCE_NOT_FOUND"
  val METHOD_NOT_ALLOWED          = "METHOD_NOT_ALLOWED"
  val ACCEPT_HEADER_INVALID       = "ACCEPT_HEADER_INVALID"
  val MESSAGE_THROTTLED_OUT       = "MESSAGE_THROTTLED_OUT"

  val SERVER_ERROR = "SERVER_ERROR"

  val INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR"
  val NOT_IMPLEMENTED       = "NOT_IMPLEMENTED"
  val SCHEDULED_MAINTENANCE = "SCHEDULED_MAINTENANCE"
  val GATEWAY_TIMEOUT       = "GATEWAY_TIMEOUT"
}
