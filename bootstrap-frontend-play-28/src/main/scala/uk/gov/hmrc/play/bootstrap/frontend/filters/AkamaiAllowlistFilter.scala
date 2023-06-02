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

package uk.gov.hmrc.play.bootstrap.frontend.filters

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


import play.api.mvc.{Call, Result, RequestHeader, Filter}
import play.api.mvc.Results._

import scala.concurrent.Future

trait AkamaiAllowlistFilter extends Filter {

  val trueClient = "True-Client-IP"

  private def isCircularDestination(requestHeader: RequestHeader): Boolean  =
    requestHeader.uri == destination.url

  private def toCall(rh: RequestHeader): Call =
    Call(rh.method, rh.uri)

  def allowlist: Seq[String]

  def excludedPaths: Seq[Call] =
    Seq.empty

  def destination: Call

  def noHeaderAction(f: RequestHeader => Future[Result], rh: RequestHeader): Future[Result] =
    Future.successful(NotImplemented)

  def response: Result = Redirect(destination)

  override def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] =
    if (excludedPaths.contains(toCall(rh)))
      f(rh)
    else
      rh.headers.get(trueClient).fold(noHeaderAction(f, rh))(ip =>
        if (allowlist.contains(ip))
          f(rh)
        else if (isCircularDestination(rh))
          Future.successful(Forbidden)
        else
          Future.successful(response)
      )
}

