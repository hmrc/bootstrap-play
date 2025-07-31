/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.play.bootstrap

import play.api.{Configuration, Environment}
import play.api.i18n.MessagesApi
import play.api.inject.Binding
import play.api.mvc.{BodyParsers, DefaultActionBuilder, MessagesActionBuilder, MessagesRequest, Request, Result}
import uk.gov.hmrc.mdc.RequestMdc

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

class BuiltinModule extends play.api.inject.Module {
  override def bindings(environment: Environment, configuration: Configuration): scala.collection.Seq[Binding[?]] =
    new play.api.inject.BuiltinModule()
      .bindings(environment, configuration)
      .map { b =>
        if (b.key.clazz.getName == classOf[DefaultActionBuilder].getName)
          bind[DefaultActionBuilder].to[BootstrapDefaultActionBuilder]
        else if (b.key.clazz.getName == classOf[MessagesActionBuilder].getName)
          bind[MessagesActionBuilder].to[BootstrapDefaultMessagesActionBuilder]
        else
          b
      }

}


@Singleton
class BootstrapDefaultActionBuilder @Inject()(
  override val parser: BodyParsers.Default
)(implicit
  override val executionContext: ExecutionContext
) extends DefaultActionBuilder {

  override def invokeBlock[T](
    request: Request[T],
    block  : Request[T] => Future[Result]
  ): Future[Result] = {
    RequestMdc.initMdc(request.id)
    block(request)
      .andThen(_ => RequestMdc.initMdc(request.id))
  }
}

@Singleton
class BootstrapDefaultMessagesActionBuilder @Inject()(
  override val parser: BodyParsers.Default,
  messagesApi        : MessagesApi
)(implicit
  override val executionContext: ExecutionContext
) extends MessagesActionBuilder {

  override def invokeBlock[T](
    request: Request[T],
    block  : MessagesRequest[T] => Future[Result]
  ): Future[Result] = {
    RequestMdc.initMdc(request.id)
    block(new MessagesRequest[T](request, messagesApi))
      .andThen(_ => RequestMdc.initMdc(request.id))
  }
}
