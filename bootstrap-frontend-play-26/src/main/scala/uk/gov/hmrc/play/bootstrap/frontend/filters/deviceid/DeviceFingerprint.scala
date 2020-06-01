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

package uk.gov.hmrc.play.bootstrap.frontend.filters.deviceid

import com.google.common.io.BaseEncoding
import play.api.Logger
import play.api.mvc.RequestHeader

import scala.util.Try

trait DeviceFingerprint {

  private val logger = Logger(getClass)

  val deviceFingerprintCookieName = "mdtpdf"

  def deviceFingerprintFrom(request: RequestHeader): String =
    request.cookies
      .get(deviceFingerprintCookieName)
      .map { cookie =>
        // TODO This can't throw an exception...
        val decodeAttempt = Try {
          BaseEncoding.base64().decode(cookie.value)
        }
        decodeAttempt.failed.foreach { e =>
          logger.info(
            s"Failed to decode device fingerprint '${cookie.value}' caused by '${e.getClass.getSimpleName}:${e.getMessage}'")
        }
        decodeAttempt
          .map {
            new String(_, "UTF-8")
          }
          .getOrElse("-")
      }
      .getOrElse("-")
}

object DeviceFingerprint extends DeviceFingerprint