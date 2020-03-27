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

import javax.inject.{Inject, Singleton}
import play.api._

import scala.annotation.tailrec

@Singleton
class RunMode @Inject()(configuration: Configuration, mode: Mode) {

  lazy val env: String =
    if (mode.equals(Mode.Test)) {
      "Test"
    } else {
      configuration.getOptional[String]("run.mode").getOrElse("Dev")
    }

  /**
    * Returns the appropriate `env` specific url given `prod` and other alternatives.
    *
    * For example, to decide which Url to return based on environment:
    * {{{
    * val url = envPath("/ping")(other = "http://localhost:9000/some-service/", prod = "")
    * }}}
    *
    * Such that, in `dev` mode, the result would be the absolute Url string `http://localhost:9000/some-service/status`
    * and in `prod` the result would be a relative Url String `/ping`
    *
    * @param path the path to append to the `prod` or `other`
    * @param other the alternative path
    * @param prod the production path
    * @return the `env` specific path
    */
  def envPath(path: String = "")(other: => String = "", prod: => String = ""): String = {

    @tailrec
    def sanitise(url: String, prefix: String = ""): String =
      if (url.startsWith("/")) sanitise(url.drop(1), prefix)
      else if (url.endsWith("/")) sanitise(url.dropRight(1), prefix)
      else if (url.isEmpty || prefix.isEmpty) url
      else prefix + url

    val url = if (env == "Prod") sanitise(prod, "/") else sanitise(other)
    url + sanitise(path, "/")
  }

}
