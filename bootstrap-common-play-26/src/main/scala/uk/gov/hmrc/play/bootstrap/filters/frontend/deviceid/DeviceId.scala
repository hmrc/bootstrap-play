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

import java.security.MessageDigest
import java.util.UUID

import org.apache.commons.codec.binary.Base64
import play.api.mvc.Cookie

import scala.util.Try

/**
  * The DeviceId is a long lived cookie which represents a digital signature composed of a UUID, timestamp in milliseconds and a hash.
  *
  * The format of the cookie 'mdtpdi' is...
  *
  *    mdtpdi#UUID#TIMESTAMP_hash
  *
  * Note the above hash is a one way hash of the value preceding the "_".
  *
  */
case class DeviceId(uuid: String, timestamp: Long, hash: String) {

  import DeviceId.{MdtpDeviceId, TenYears, Token1, Token2}

  def value = s"$MdtpDeviceId$Token1$uuid$Token1$timestamp$Token2$hash"

  def cookie = Cookie(DeviceId.MdtpDeviceId, value, Some(TenYears), secure = true)
}

object DeviceId {

  val Token1       = "#"
  val Token2       = "_"
  val TenYears     = 315360000
  val MdtpDeviceId = "mdtpdi"

  def generateHash(uuid: String, timestamp: Long, secret: String) = {
    val oneWayHash = s"$MdtpDeviceId$Token1$uuid$Token1$timestamp"
    val digest     = MessageDigest.getInstance("MD5").digest((oneWayHash + secret).getBytes)
    new String(Base64.encodeBase64(digest))
  }

  def deviceIdHashIsValid(hash: String, uuid: String, timestamp: Long, secret: String, previousSecrets: Seq[String]) = {
    val secrets     = Seq(secret) ++ previousSecrets
    val hashChecker = secrets.map(item => () => hash == generateHash(uuid, timestamp, item)).toStream
    hashChecker.map(_()).collectFirst { case true => true }.getOrElse(false)
  }

  def from(value: String, secret: String, previousSecrets: Seq[String]) = {

    def isValidPrefix(prefix: String) = prefix == MdtpDeviceId

    def isValid(prefix: String, uuid: String, timestamp: String, hash: String) =
      isValidPrefix(prefix) && validUuid(uuid) && validLongTime(timestamp) && deviceIdHashIsValid(
        hash,
        uuid,
        timestamp.toLong,
        secret,
        previousSecrets)

    value.split("(#)|(_)") match {
      case Array(prefix, uuid, timestamp, hash) if isValid(prefix, uuid, timestamp, hash) =>
        Some(DeviceId(uuid, timestamp.toLong, hash))
      case _ => None
    }
  }

  private def validUuid(uuid: String) = Try { UUID.fromString(uuid) }.isSuccess

  private def validLongTime(timestamp: String) = Try { timestamp.toLong }.isSuccess

}
