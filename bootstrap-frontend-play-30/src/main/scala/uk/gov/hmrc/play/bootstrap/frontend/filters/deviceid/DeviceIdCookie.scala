/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.play.bootstrap.frontend.filters.deviceid

import java.util.UUID

import play.api.mvc.Cookie

trait DeviceIdCookie {
  def secret         : String
  def previousSecrets: Seq[String]
  def secure         : Boolean

  def getTimeStamp(): Long =
    System.currentTimeMillis()

  def generateUUID(): String =
    UUID.randomUUID().toString

  def generateDeviceId(uuid: String = generateUUID()): DeviceId = {
    val timestamp = getTimeStamp()
    DeviceId(uuid, timestamp, DeviceId.generateHash(uuid, timestamp, secret))
  }

  def buildNewDeviceIdCookie(): Cookie =
    makeCookie(generateDeviceId())

  def makeCookie(deviceId: DeviceId): Cookie =
    Cookie(
      name   = DeviceId.MdtpDeviceId,
      value  = deviceId.value,
      maxAge = Some(DeviceId.TenYears),
      secure = secure
    )
}
