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

package uk.gov.hmrc.play.bootstrap.filters

import akka.stream._
import akka.stream.stage._
import akka.util.ByteString
import play.api.Logger

// based on play.filters.csrf.CSRFAction#BodyHandler

protected[filters] class RequestBodyCaptor(
  val loggingContext: String,
  val maxBodyLength: Int,
  callback: (ByteString) => Unit
) extends GraphStage[FlowShape[ByteString, ByteString]] {
  val in             = Inlet[ByteString]("ReqBodyCaptor.in")
  val out            = Outlet[ByteString]("ReqBodyCaptor.out")
  override val shape = FlowShape.of(in, out)

  private val logger = Logger(getClass)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    private var buffer: ByteString = ByteString.empty
    private var bodyLength         = 0

    setHandlers(
      in,
      out,
      new InHandler with OutHandler {

        override def onPull(): Unit =
          pull(in)

        override def onPush(): Unit = {
          val chunk = grab(in)
          bodyLength += chunk.length
          if (buffer.size < maxBodyLength)
            buffer ++= chunk
          push(out, chunk)
        }

        override def onUpstreamFinish(): Unit = {
          if (bodyLength > maxBodyLength)
            logger.warn(
              s"txm play auditing: $loggingContext sanity check request body $bodyLength exceeds maxLength $maxBodyLength - do you need to be auditing this payload?")
          callback(buffer.take(maxBodyLength))
          if (isAvailable(out) && buffer == ByteString.empty)
            push(out, buffer)
          completeStage()
        }
      }
    )
  }
}

protected[filters] class ResponseBodyCaptor(
  val loggingContext: String,
  val maxBodyLength: Int,
  performAudit: (String) => Unit
) extends GraphStage[SinkShape[ByteString]] {
  val in             = Inlet[ByteString]("RespBodyCaptor.in")
  override val shape = SinkShape.of(in)

  private val logger = Logger(getClass)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    private var buffer: ByteString = ByteString.empty
    private var bodyLength         = 0

    override def preStart(): Unit = pull(in)

    setHandler(
      in,
      new InHandler {

        override def onPush(): Unit = {
          val chunk = grab(in)
          bodyLength += chunk.length
          if (buffer.size < maxBodyLength)
            buffer ++= chunk
          pull(in)
        }

        override def onUpstreamFinish(): Unit = {
          if (bodyLength > maxBodyLength)
            logger.warn(
              s"txm play auditing: $loggingContext sanity check request body $bodyLength exceeds maxLength $maxBodyLength - do you need to be auditing this payload?")
          performAudit(buffer.take(maxBodyLength).decodeString("UTF-8"))
          completeStage()
        }

        override def onUpstreamFailure(ex: Throwable): Unit = {
          performAudit("")
          super.onUpstreamFailure(ex)
        }
      }
    )
  }
}
