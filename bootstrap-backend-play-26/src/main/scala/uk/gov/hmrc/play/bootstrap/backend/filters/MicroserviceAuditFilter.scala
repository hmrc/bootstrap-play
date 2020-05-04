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

package uk.gov.hmrc.play.bootstrap.backend.filters

import akka.stream._
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.util.ByteString
import javax.inject.Inject
import play.api.Logger
import play.api.http.HttpEntity
import play.api.http.HttpEntity.Streamed
import play.api.libs.streams.Accumulator
import play.api.mvc.{Result, _}
import play.api.routing.Router.Attrs
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.audit.EventKeys._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.bootstrap.config.{ControllerConfigs, HttpAuditEvent}
import uk.gov.hmrc.play.bootstrap.filters.AuditFilter
import uk.gov.hmrc.play.bootstrap.filters.microservice.{RequestBodyCaptor, ResponseBodyCaptor}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

trait MicroserviceAuditFilter extends AuditFilter {

  protected implicit def ec: ExecutionContext
  def auditConnector: AuditConnector

  def controllerNeedsAuditing(controllerName: String): Boolean

  def dataEvent(
    eventType: String,
    transactionName: String,
    request: RequestHeader,
    detail: Map[String, String] = Map())(
    implicit hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)
  ): DataEvent

  implicit def mat: Materializer

  val maxBodySize = 32665

  val requestReceived = "RequestReceived"

  def apply(nextFilter: EssentialAction) = new EssentialAction {
    def apply(requestHeader: RequestHeader) = {
      val next: Accumulator[ByteString, Result] = nextFilter(requestHeader)
      implicit val hc                           = HeaderCarrierConverter.fromHeadersAndSession(requestHeader.headers, Some(requestHeader.session))

      val loggingContext = s"${requestHeader.method} ${requestHeader.uri}"

      def performAudit(requestBody: String, maybeResult: Try[Result])(responseBody: String): Unit =
        maybeResult match {
          case Success(result) =>
            auditConnector.sendEvent(
              dataEvent(
                requestReceived,
                requestHeader.uri,
                requestHeader,
                Map(ResponseMessage -> responseBody, StatusCode -> result.header.status.toString)))
          case Failure(f) =>
            auditConnector.sendEvent(
              dataEvent(requestReceived, requestHeader.uri, requestHeader, Map(FailedRequestMessage -> f.getMessage)))
        }

      if (needsAuditing(requestHeader)) {
        onCompleteWithInput(loggingContext, next, performAudit)
      } else next
    }
  }

  protected def needsAuditing(request: RequestHeader): Boolean =
    request.attrs.get(Attrs.HandlerDef).forall { handlerDef =>
      controllerNeedsAuditing(handlerDef.controller)
    }

  protected def onCompleteWithInput(
    loggingContext: String,
    next: Accumulator[ByteString, Result],
    handler: (String, Try[Result]) => String => Unit)(
    implicit ec: ExecutionContext): Accumulator[ByteString, Result] = {
    val requestBodyPromise = Promise[String]()
    val requestBodyFuture  = requestBodyPromise.future

    var requestBody: String = ""
    def callback(body: ByteString): Unit = {
      requestBody = body.decodeString("UTF-8")
      requestBodyPromise success requestBody
    }

    //grabbed from plays csrf filter
    val wrappedAcc: Accumulator[ByteString, Result] = Accumulator(
      Flow[ByteString]
        .via(new RequestBodyCaptor(loggingContext, maxBodySize, callback))
        .splitWhen(_ => false)
        .prefixAndTail(0)
        .map(_._2)
        .concatSubstreams
        .toMat(Sink.head[Source[ByteString, _]])(Keep.right)
    ).mapFuture { bodySource =>
      next.run(bodySource)
    }

    wrappedAcc
      .mapFuture { result =>
        requestBodyFuture flatMap { res =>
          {
            val auditedBody = result.body match {
              case str: Streamed => {
                val auditFlow = Flow[ByteString].alsoTo(
                  new ResponseBodyCaptor(loggingContext, maxBodySize, handler(requestBody, Success(result))))
                str.copy(data = str.data.via(auditFlow))
              }
              case h: HttpEntity => {
                h.consumeData map { rb =>
                  val auditString = if (rb.size > maxBodySize) {
                    Logger.warn(
                      s"txm play auditing: $loggingContext response body ${rb.size} exceeds maxLength $maxBodySize - do you need to be auditing this payload?")
                    rb.take(maxBodySize).decodeString("UTF-8")
                  } else {
                    rb.decodeString("UTF-8")
                  }
                  handler(res, Success(result))(auditString)
                }
                h
              }
            }
            Future(result.copy(body = auditedBody))
          }
        }
      }
      .recover[Result] {
        case ex: Throwable =>
          handler(requestBody, Failure(ex))("")
          throw ex
      }
  }
}

class DefaultMicroserviceAuditFilter @Inject()(
  controllerConfigs: ControllerConfigs,
  override val auditConnector: AuditConnector,
  httpAuditEvent: HttpAuditEvent,
  override val mat: Materializer
)(implicit protected val ec: ExecutionContext) extends MicroserviceAuditFilter {

  override def controllerNeedsAuditing(controllerName: String): Boolean =
    controllerConfigs.controllerNeedsAuditing(controllerName)

  override def dataEvent(
    eventType: String,
    transactionName: String,
    request: RequestHeader,
    detail: Map[String, String])(implicit hc: HeaderCarrier): DataEvent =
    httpAuditEvent.dataEvent(eventType, transactionName, request, detail)
}
