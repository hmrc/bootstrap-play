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

package health

import javax.inject.Inject

import play.api.routing.{HandlerDef, Router}
import play.api.routing.sird._
import play.core.routing.GeneratedRouter
import uk.gov.hmrc.play.health.HealthController

class Routes @Inject()(
  val errorHandler: play.api.http.HttpErrorHandler,
  healthController: HealthController,
  prefix: String
) extends GeneratedRouter {

  override def documentation: Seq[(String, String, String)] =
    Seq.empty

  override def withPrefix(addPrefix: String): Router =
    new Routes(errorHandler, healthController, prefix + addPrefix)

  override def routes: Router.Routes = {
    case GET(p"/ping/ping") =>
      // Audit/Auth/Logging filters rely on HandlerDef configuration (controller)
      // in order to attach it to the request, we must extend GeneratedRouter and use the invoker tricks..
      createInvoker(
        fakeCall   = healthController.ping,
        handlerDef = HandlerDef(
          classLoader    = this.getClass.getClassLoader,
          routerPackage  = "app",
          controller     = "uk.gov.hmrc.play.health.HealthController",
          method         = "ping",
          parameterTypes = Nil,
          verb           = "GET",
          path           = this.prefix + "ping/ping",
          comments       = "",
          modifiers      = Seq()
        )
      ).call(healthController.ping)
  }
}
