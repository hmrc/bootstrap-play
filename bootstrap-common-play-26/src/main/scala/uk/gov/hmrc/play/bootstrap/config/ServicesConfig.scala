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

package uk.gov.hmrc.play.bootstrap.config

import javax.inject.Inject
import play.api.Configuration

import scala.concurrent.duration.Duration

class ServicesConfig @Inject()(configuration: Configuration) {

  protected lazy val rootServices = "microservice.services"

  protected lazy val defaultProtocol: String =
    configuration
      .getOptional[String](s"$rootServices.protocol")
      .getOrElse("http")

  protected def config(serviceName: String) =
    configuration
      .getOptional[Configuration](s"$rootServices.$serviceName")
      .getOrElse(throw new IllegalArgumentException(s"Configuration for service $serviceName not found"))

  def baseUrl(serviceName: String) = {
    val protocol = getConfString(s"$serviceName.protocol", defaultProtocol)
    val host     = getConfString(s"$serviceName.host", throwConfigNotFoundError(s"$serviceName.host"))
    val port     = getConfInt(s"$serviceName.port", throwConfigNotFoundError(s"$serviceName.port"))
    s"$protocol://$host:$port"
  }

  def getConfString(confKey: String, defString: => String) =
    configuration
      .getOptional[String](s"$rootServices.$confKey")
      .getOrElse(defString)

  def getConfInt(confKey: String, defInt: => Int) =
    configuration
      .getOptional[Int](s"$rootServices.$confKey")
      .getOrElse(defInt)

  def getConfBool(confKey: String, defBool: => Boolean) =
    configuration
      .getOptional[Boolean](s"$rootServices.$confKey")
      .getOrElse(defBool)

  def getConfDuration(confKey: String, defDur: => Duration) =
    configuration
      .getOptional[String](s"$rootServices.$confKey")
      .map(Duration.create)
      .getOrElse(defDur)

  def getInt(key: String) =
    configuration.getOptional[Int](key).getOrElse(throwConfigNotFoundError(key))

  def getString(key: String) =
    configuration.getOptional[String](key).getOrElse(throwConfigNotFoundError(key))

  def getBoolean(key: String) =
    configuration.getOptional[Boolean](key).getOrElse(throwConfigNotFoundError(key))

  def getDuration(key: String) =
    configuration.getOptional[String](key).map(Duration.create).getOrElse(throwConfigNotFoundError(key))

  private def throwConfigNotFoundError(key: String) =
    throw new RuntimeException(s"Could not find config key '$key'")
}
