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

package uk.gov.hmrc.play.bootstrap.http.utils

import java.net.ServerSocket

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpec

import scala.util.{Random, Try}

trait WiremockTestServer extends AnyWordSpec with BeforeAndAfterAll {

  val wireMockPort   = PortTester.findPort()
  val wireMockServer = new WireMockServer(wireMockPort)

  override protected def beforeAll(): Unit = {
    wireMockServer.start()
    WireMock.configureFor("localhost", wireMockPort)
  }

  override protected def afterAll(): Unit =
    wireMockServer.stop()
}

private object PortTester {

  def findPort(excluded: Int*): Int =
    Random.shuffle((20001 to 20100).toVector).find(port => !excluded.contains(port) && isFree(port)).getOrElse(throw new Exception("No free port"))

  private def isFree(port: Int): Boolean = {
    val triedSocket = Try {
      val serverSocket = new ServerSocket(port)
      Try(serverSocket.close())
      serverSocket
    }
    triedSocket.isSuccess
  }
}
