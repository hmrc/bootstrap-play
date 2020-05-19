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

import play.api.{Configuration, Logger}

class DeprecatedConfigChecker(
  configuration: Configuration,
  deprecatedValues: Map[String, String]
) {

  private val logger = Logger(getClass)

  val stringKeys =
    List(
      "play.http.errorHandler",
      "play.http.filters"
    )

  val stringArrayKeys =
    List(
      "play.modules.disabled",
      "play.modules.enabled"
    )

  def checkForDeprecatedValue(key: String): Option[(String,String,String)] =
    configuration.getOptional[String](key)
      .flatMap(d => deprecatedValues.get(d).map((key, d, _)))

  def checkForDeprecatedValues(key: String): Seq[(String,String,String)] =
    configuration.getOptional[Seq[String]](key).toSeq
      .flatMap(_.flatMap(d => deprecatedValues.get(d).map((key, d, _)).toSeq))

  def deprecations: Seq[(String, String, String)] =
    stringKeys.flatMap(checkForDeprecatedValue) ++
    stringArrayKeys.flatMap(checkForDeprecatedValues)

  deprecations.map { case (k,d,v) =>
    logger.warn(s"The key '$k' is configured with value '$d', which is deprecated. Please use '$v' instead.")
  }
}
