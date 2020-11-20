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

  deprecations.foreach { case (k,d,v) =>
    logger.warn(s"The key '$k' is configured with value '$d', which is deprecated. Please use '$v' instead.")
  }

  val deprecatedKeys = List(
    "httpHeadersWhitelist"                    -> "bootstrap.http.headersAllowlist",
    "bootstrap.filters.whitelist.destination" -> "bootstrap.filters.allowlist.destination",
    "bootstrap.filters.whitelist.excluded"    -> "bootstrap.filters.allowlist.excluded",
    "bootstrap.filters.whitelist.ips"         -> "bootstrap.filters.allowlist.ips",
  )

  val errs = deprecatedKeys.filter { case (d, _) => configuration.has(d) }
  if (errs.nonEmpty) {
    if (configuration.get[Boolean]("bootstrap.configuration.failOnObsoleteKeys"))
      throw configuration.globalError(
        "The following configurations keys were found which are obsolete. Their presence indicate misconfiguration. You must remove them or use the suggested alternatives:\n" +
          errs.map { case (d, k) =>  s"'$d' - Please use '$k' instead" }.mkString("\n")
      )
    else
      errs.foreach { case (d, k) =>
        logger.warn(s"The configuration key '$d' is no longer supported and is being IGNORED! Please use '$k' instead.")
      }
  }
}
