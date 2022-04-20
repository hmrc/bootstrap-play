/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.{Configuration, Logger}
import play.api.http.HttpChunk
import play.api.mvc.EssentialFilter
import akka.stream._
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.util.ByteString
import play.api.http.HttpEntity
import play.api.libs.streams.Accumulator
import play.api.mvc._
import play.api.routing.Router.Attrs
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.hooks.Body
import uk.gov.hmrc.play.audit.EventKeys
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{DataEvent, TruncationLog}
import uk.gov.hmrc.play.http.BodyCaptor

import scala.concurrent.{ExecutionContext, Promise}

trait AuditFilter extends EssentialFilter

trait CommonAuditFilter extends AuditFilter {
  protected implicit def ec: ExecutionContext

  def config: Configuration

  def auditConnector: AuditConnector

  def controllerNeedsAuditing(controllerName: String): Boolean

  def dataEvent(
    eventType      : String,
    transactionName: String,
    request        : RequestHeader,
    detail         : Map[String, String]   = Map.empty,
    truncationLog  : Option[TruncationLog] = None
  )(implicit
    hc: HeaderCarrier
  ): DataEvent

  implicit def mat: Materializer

  private val logger = Logger(getClass)

  val maxBodySize = config.get[Int]("bootstrap.auditing.maxBodyLength")

  val requestReceived = "RequestReceived"

  implicit protected def hc(implicit request: RequestHeader): HeaderCarrier

  override def apply(nextFilter: EssentialAction) = new EssentialAction {
    override def apply(requestHeader: RequestHeader): Accumulator[ByteString, Result] = {
      val next: Accumulator[ByteString, Result] = nextFilter(requestHeader)
      if (needsAuditing(requestHeader))
        onCompleteWithInput(next, performAudit(requestHeader))
      else
        next
    }
  }

  private def performAudit(requestHeader: RequestHeader)(requestBody: Body[String], result: Either[Throwable, (Result, Body[String])]): Unit = {
    val (detail, truncationLog) =
      result match {
        case Right((result, responseBody)) =>
          val (requestDetail , requestTruncationLog ) = buildRequestDetails(requestHeader, requestBody)
          val (responseDetail, responseTruncationLog) = buildResponseDetails(result.header, responseBody, result.body.contentType)
          ( requestDetail ++ responseDetail,
            TruncationLog(requestTruncationLog.truncatedFields ++ responseTruncationLog.truncatedFields)
          )
        case Left(ex) =>
          val (requestDetail, requestTruncationLog) = buildRequestDetails(requestHeader, requestBody)
          ( requestDetail ++ Map(EventKeys.FailedRequestMessage -> ex.getMessage),
            requestTruncationLog
          )
      }

    val truncationLog2 = TruncationLog(truncatedFields = truncationLog.truncatedFields.map("detail." + _))
    if (truncationLog2.truncatedFields.nonEmpty)
      logger.info(s"Inbound ${requestHeader.method} ${requestHeader.uri} - the following fields were truncated for auditing: ${truncationLog2.truncatedFields.mkString(", ")}")

    implicit val r = requestHeader
    auditConnector.sendEvent(
      dataEvent(
        eventType       = requestReceived,
        transactionName = requestHeader.uri,
        request         = requestHeader,
        detail          = detail,
        truncationLog   = Some(truncationLog2)
      )
    )
  }

  protected def needsAuditing(request: RequestHeader): Boolean =
    config.get[Boolean]("auditing.enabled") &&
    request.attrs.get(Attrs.HandlerDef).map(_.controller).forall(controllerNeedsAuditing)

  protected def onCompleteWithInput(
    next          : Accumulator[ByteString, Result],
    handler       : (Body[String], Either[Throwable, (Result, Body[String])]) => Unit
  )(implicit ec: ExecutionContext
  ): Accumulator[ByteString, Result] = {
    val requestBodyPromise  = Promise[Body[String]]()

    // grabbed from plays csrf filter (play.filters.csrf.CSRFAction#checkBody https://github.com/playframework/playframework/blob/2.8.13/web/play-filters-helpers/src/main/scala/play/filters/csrf/CSRFActions.scala#L161-L185)
    // we don't just use `next.through(BodyCaptor.flow)` since the stream wouldn't be audited without the controller pulling the content
    val wrappedAcc: Accumulator[ByteString, Result] =
      Accumulator(
        Flow[ByteString]
          .via(BodyCaptor.flow(
            maxBodySize,
            withCapturedBody = body => requestBodyPromise.success(body.map(_.utf8String))
          ))
          .splitWhen(_ => false)
          .prefixAndTail(0)
          .map(_._2)
          .concatSubstreams
          .toMat(Sink.headOption[Source[ByteString, _]])(Keep.right)
          .mapMaterializedValue(_.map(_.getOrElse(Source.single(ByteString.empty))))
      ).mapFuture(next.run)

    wrappedAcc
      .map { result =>
        val responseBodyPromise = Promise[Body[String]]()

        lazy val responseBodyCaptor: Sink[ByteString, akka.NotUsed] =
          BodyCaptor.sink(
            maxBodySize,
            withCapturedBody = body => responseBodyPromise.success(body.map(_.utf8String))
          )

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
              responseBodyPromise.success(BodyCaptor.bodyUpto(rb, maxBodySize).map(_.utf8String))
            }
            h
        }

        for {
          auditRequestBody  <- requestBodyPromise.future
          auditResponseBody <- responseBodyPromise.future
        } yield handler(auditRequestBody, Right((result, auditResponseBody)))

        result.copy(body = auditedBody)
      }
      .recover[Result] {
        case ex: Throwable =>
          for {
            auditRequestBody  <- requestBodyPromise.future
          } yield handler(auditRequestBody, Left(ex))
          throw ex
      }
  }

  protected def buildRequestDetails(requestHeader: RequestHeader, requestBody: Body[String]): (Map[String, String], TruncationLog)

  protected def buildResponseDetails(responseHeader: ResponseHeader, responseBody: Body[String], contentType: Option[String]): (Map[String, String], TruncationLog)
}
