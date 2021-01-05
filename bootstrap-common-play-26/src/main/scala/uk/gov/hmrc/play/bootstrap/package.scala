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

package uk.gov.hmrc.play

import uk.gov.hmrc.play.bootstrap.binders.{AbsoluteWithHostnameFromAllowlist, RedirectUrlPolicy}
import scala.concurrent.{ExecutionContext, Future}

package boostrap {

  package object binders {

    @deprecated("Use uk.gov.hmrc.play.bootstrap.binders.AbsoluteWithHostnameFromAllowlist instead", "4.0.0")
    object AbsoluteWithHostnameFromWhitelist {

      def apply(allowedHosts: String*): RedirectUrlPolicy[RedirectUrlPolicy.Id] =
        apply(allowedHosts.toSet)

      def apply(allowedHosts: Set[String]): RedirectUrlPolicy[RedirectUrlPolicy.Id] =
        AbsoluteWithHostnameFromAllowlist.apply(allowedHosts)

      def apply(allowedHostsFn: => Future[Set[String]])(implicit ec: ExecutionContext): RedirectUrlPolicy[Future] =
        AbsoluteWithHostnameFromAllowlist.apply(allowedHostsFn)
    }
  }
}
