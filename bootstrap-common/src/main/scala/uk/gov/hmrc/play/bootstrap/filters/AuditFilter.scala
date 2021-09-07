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

import play.api.Configuration
import play.api.mvc.EssentialFilter
import akka.stream._
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.util.ByteString
import play.api.Logger
import play.api.http.HttpEntity
import play.api.libs.streams.Accumulator
import play.api.mvc._
import play.api.routing.Router.Attrs
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.EventKeys
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}
import play.api.http.HttpChunk

trait AuditFilter extends EssentialFilter

trait CommonAuditFilter extends AuditFilter {
  private val logger = Logger(getClass)

  protected implicit def ec: ExecutionContext

  def config: Configuration

  def auditConnector: AuditConnector

  def controllerNeedsAuditing(controllerName: String): Boolean

  def dataEvent(
    eventType      : String,
    transactionName: String,
    request        : RequestHeader,
    detail         : Map[String, String] = Map()
  )(implicit
    hc: HeaderCarrier
  ): DataEvent

  implicit def mat: Materializer

  val maxBodySize = 32665

  val requestReceived = "RequestReceived"

  implicit protected def hc(implicit request: RequestHeader): HeaderCarrier

  override def apply(nextFilter: EssentialAction) = new EssentialAction {
    override def apply(requestHeader: RequestHeader): Accumulator[ByteString, Result] = {
      val next: Accumulator[ByteString, Result] = nextFilter(requestHeader)

      val loggingContext = s"${requestHeader.method} ${requestHeader.uri}"

      def performAudit(requestBody: String, tryResult: Try[Result])(responseBody: String): Unit = {
        val detail = tryResult match {
          case Success(result) =>
            val responseHeader = result.header
            Map(
                EventKeys.ResponseMessage -> filterResponseBody(result, responseHeader, responseBody),
                EventKeys.StatusCode      -> responseHeader.status.toString
              ) ++
              buildRequestDetails(requestHeader, requestBody) ++
              buildResponseDetails(responseHeader)
          case Failure(f) =>
            Map(EventKeys.FailedRequestMessage -> f.getMessage) ++
              buildRequestDetails(requestHeader, requestBody)
        }
        implicit val r = requestHeader
        auditConnector.sendEvent(
          dataEvent(
            eventType       = requestReceived,
            transactionName = requestHeader.uri,
            request         = requestHeader,
            detail          = detail
          )
        )
      }

      if (needsAuditing(requestHeader))
        onCompleteWithInput(loggingContext, next, performAudit)
      else
        next
    }
  }

  protected def needsAuditing(request: RequestHeader): Boolean =
    config.get[Boolean]("auditing.enabled") &&
    request.attrs.get(Attrs.HandlerDef).map(_.controller).forall(controllerNeedsAuditing)

  protected def onCompleteWithInput(
    loggingContext: String,
    next: Accumulator[ByteString, Result],
    handler: (String, Try[Result]) => String => Unit
  )(implicit ec: ExecutionContext
  ): Accumulator[ByteString, Result] = {
    val requestBodyPromise = Promise[String]()
    val requestBodyFuture  = requestBodyPromise.future

    var requestBody: String = ""
    def callback(body: ByteString): Unit = {
      requestBody = body.decodeString("UTF-8")
      requestBodyPromise.success(requestBody)
    }

    //grabbed from plays csrf filter (play.filters.csrf.CSRFAction#checkBody)
    val wrappedAcc: Accumulator[ByteString, Result] =
      Accumulator(
        Flow[ByteString]
          .via(new RequestBodyCaptor(loggingContext, maxBodySize, callback))
          .splitWhen(_ => false)
          .prefixAndTail(0)
          .map(_._2)
          .concatSubstreams
          .toMat(Sink.head[Source[ByteString, _]])(Keep.right)
      ).mapFuture(next.run)

    wrappedAcc
      .mapFuture { result =>
        lazy val responseBodyCaptor: Sink[ByteString, akka.NotUsed] =
          Sink.fromGraph(new ResponseBodyCaptor(
            loggingContext,
            maxBodySize,
            performAudit = handler(requestBody, Success(result))
          ))

        requestBodyFuture.flatMap { res =>
          val auditedBody = result.body match {
            case str: HttpEntity.Streamed =>
              str.copy(data = str.data.alsoTo(responseBodyCaptor))
            case str: HttpEntity.Chunked =>
              val chunkedResponseBodyCaptor: Sink[HttpChunk, akka.NotUsed] =
                responseBodyCaptor.contramap {
                  case HttpChunk.Chunk(data)  => data
                  case HttpChunk.LastChunk(_) => ByteString()
                }
              str.copy(chunks = str.chunks.alsoTo(chunkedResponseBodyCaptor))
            case h: HttpEntity =>
              h.consumeData.map { rb =>
                val auditString =
                  if (rb.size > maxBodySize) {
                    logger.warn(
                      s"txm play auditing: $loggingContext response body ${rb.size} exceeds maxLength $maxBodySize - do you need to be auditing this payload?")
                    rb.take(maxBodySize).decodeString("UTF-8")
                  } else
                    rb.decodeString("UTF-8")
                handler(res, Success(result))(auditString)
              }
              h
          }
          Future(result.copy(body = auditedBody))
        }
      }
      .recover[Result] {
        case ex: Throwable =>
          handler(requestBody, Failure(ex))("")
          throw ex
      }
  }

  protected def filterResponseBody(result: Result, response: ResponseHeader, responseBody: String): String

  protected def buildRequestDetails(requestHeader: RequestHeader, requestBody: String): Map[String, String]

  protected def buildResponseDetails(response: ResponseHeader): Map[String, String]
}
