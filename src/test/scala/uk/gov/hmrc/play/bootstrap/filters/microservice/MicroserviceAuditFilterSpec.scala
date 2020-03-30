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

package uk.gov.hmrc.play.bootstrap.filters.microservice

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import play.api.mvc.Results.NotFound
import play.api.mvc.{Action, AnyContent}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.bootstrap.config.{ControllerConfigs, HttpAuditEvent}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class MicroserviceAuditFilterSpec extends WordSpec with Matchers with Eventually with ScalaFutures with MockitoSugar {

  "AuditFilter" should {
    val applicationName = "app-name"

    val requestReceived  = "RequestReceived"
    val xRequestId       = "A_REQUEST_ID"
    val xSessionId       = "A_SESSION_ID"
    val deviceID         = "A_DEVICE_ID"
    val akamaiReputation = "AN_AKAMAI_REPUTATION"

    implicit val system       = ActorSystem()
    implicit val materializer = ActorMaterializer()

    val request = FakeRequest().withHeaders(
      "X-Request-ID"      -> xRequestId,
      "X-Session-ID"      -> xSessionId,
      "deviceID"          -> deviceID,
      "Akamai-Reputation" -> akamaiReputation)

    val controllerConfigs = mock[ControllerConfigs]
    when(controllerConfigs.controllerNeedsAuditing(anyString())).thenReturn(true)

    val httpAuditEvent = new HttpAuditEvent { override def appName = applicationName }

    def createAuditFilter(connector: AuditConnector) =
      new DefaultMicroserviceAuditFilter(controllerConfigs, connector, httpAuditEvent, materializer)

    "audit a request and response with header information" in {
      val mockAuditConnector = mock[AuditConnector]
      val auditFilter        = createAuditFilter(mockAuditConnector)

      when(mockAuditConnector.sendEvent(any[DataEvent])(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future {
        Success
      })

      val result = await(auditFilter.apply(nextAction)(request).run)

      await(result.body.dataStream.runForeach({ i =>
        }))

      eventually {
        val captor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(mockAuditConnector).sendEvent(captor.capture)(any[HeaderCarrier], any[ExecutionContext])
        verifyNoMoreInteractions(mockAuditConnector)
        val event = captor.getValue

        event.auditSource               shouldBe applicationName
        event.auditType                 shouldBe requestReceived
        event.tags("X-Request-ID")      shouldBe xRequestId
        event.tags("X-Session-ID")      shouldBe xSessionId
        event.tags("Akamai-Reputation") shouldBe akamaiReputation
        event.detail("deviceID")        shouldBe deviceID
        event.detail("responseMessage") shouldBe actionNotFoundMessage
      }
    }

    "audit a response even when an action further down the chain throws an exception" in {
      val mockAuditConnector = mock[AuditConnector]
      val auditFilter        = createAuditFilter(mockAuditConnector)

      when(mockAuditConnector.sendEvent(any[DataEvent])(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future {
        Success
      })

      a[RuntimeException] should be thrownBy await(auditFilter.apply(exceptionThrowingAction)(request).run)

      eventually {
        val captor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(mockAuditConnector).sendEvent(captor.capture)(any[HeaderCarrier], any[ExecutionContext])
        verifyNoMoreInteractions(mockAuditConnector)
        val event = captor.getValue

        event.auditSource               shouldBe applicationName
        event.auditType                 shouldBe requestReceived
        event.tags("X-Request-ID")      shouldBe xRequestId
        event.tags("X-Session-ID")      shouldBe xSessionId
        event.tags("Akamai-Reputation") shouldBe akamaiReputation
        event.detail("deviceID")        shouldBe deviceID
      }
    }
  }

  private val Action = stubControllerComponents().actionBuilder

  private val actionNotFoundMessage = "404 Not Found"

  private val nextAction: Action[AnyContent] = Action(NotFound(actionNotFoundMessage))

  private val exceptionThrowingAction = Action.async { _ =>
    throw new RuntimeException("Something went wrong")
  }

}
