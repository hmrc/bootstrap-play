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

package uk.gov.hmrc.play.bootstrap.filters.frontend.deviceid

import java.util.UUID

import play.api.mvc.Cookie

trait DeviceIdCookie {
  val secret: String
  val previousSecrets: Seq[String]

  def getTimeStamp = System.currentTimeMillis()

  def generateUUID = UUID.randomUUID().toString

  def generateDeviceId(uuid: String = generateUUID) = {
    val timestamp = getTimeStamp
    DeviceId(uuid, timestamp, DeviceId.generateHash(uuid, timestamp, secret))
  }

  def buildNewDeviceIdCookie() = {
    val deviceId = generateDeviceId()
    makeCookie(deviceId)
  }

  def makeCookie(deviceId: DeviceId) =
    Cookie(DeviceId.MdtpDeviceId, deviceId.value, Some(DeviceId.TenYears), secure = true)
}
