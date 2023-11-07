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

package uk.gov.hmrc.play.bootstrap.dispatchers

import java.util.concurrent.{ExecutorService, ThreadFactory}

import org.apache.pekko.dispatch._
import com.typesafe.config.Config
import org.slf4j.MDC

class MDCPropagatingExecutorServiceConfigurator(
  config       : Config,
  prerequisites: DispatcherPrerequisites
) extends ExecutorServiceConfigurator(config, prerequisites) {

  class MDCPropagatingExecutorServiceFactory(delegate: ExecutorServiceFactory) extends ExecutorServiceFactory {
    override def createExecutorService: ExecutorService =
      new MDCPropagatingExecutorService(delegate.createExecutorService)
  }

  override def createExecutorServiceFactory(id: String, threadFactory: ThreadFactory): ExecutorServiceFactory = {

    val factory = new ForkJoinExecutorConfigurator(config.getConfig("fork-join-executor"), prerequisites)
      .createExecutorServiceFactory(id, threadFactory)

    new MDCPropagatingExecutorServiceFactory(factory)
  }
}

class MDCPropagatingExecutorService(val executor: ExecutorService) extends ExecutorServiceDelegate {

  override def execute(command: Runnable): Unit = {
    val mdcData = MDC.getCopyOfContextMap

    executor.execute { () =>
      val oldMdcData = MDC.getCopyOfContextMap
      setMDC(mdcData)
      try {
        command.run()
      } finally {
        setMDC(oldMdcData)
      }
    }
  }

  private def setMDC(context: java.util.Map[String, String]): Unit =
    if (context == null)
      MDC.clear()
    else
      MDC.setContextMap(context)
}
