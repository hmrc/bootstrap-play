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

package uk.gov.hmrc.play.bootstrap.http.utils

import org.apache.commons.io.IOUtils
import play.api.libs.json.Json

object JsonPayloads {

  val bankHolidays = IOUtils.toString(getClass.getClassLoader.getResourceAsStream("bankHolidays.json"), "UTF-8")
  val userId       = IOUtils.toString(getClass.getClassLoader.getResourceAsStream("userId.json"), "UTF-8")
}

case class BankHolidays(events: Seq[BankHoliday])
case class BankHoliday(title: String)

object BankHolidays {

  implicit val bhr = Json.reads[BankHoliday]
  val reads        = Json.reads[BankHolidays]
}

case class User(email: String, fullName: String)

object User {
  val writes = Json.writes[User]
}

case class UserIdentifier(id: String)

object UserIdentifier {
  val reads = Json.reads[UserIdentifier]
}
