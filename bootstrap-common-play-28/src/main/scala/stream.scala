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

package uk.gov.hmrc.play.bootstrap {
  package object stream {
    private [bootstrap] type ActorSystem  = akka.actor.ActorSystem
    private [bootstrap] type ByteString   = akka.util.ByteString
    private [bootstrap] type Materializer = akka.stream.Materializer
    private [bootstrap] type Source[A, B] = akka.stream.scaladsl.Source[A, B]
    private [bootstrap] type Done         = akka.Done

    private [bootstrap] def ActorSystem(name: String): ActorSystem =
      akka.actor.ActorSystem.apply(name)

    private [bootstrap] object ByteString {
      def apply(s: String): ByteString =
        akka.util.ByteString.apply(s)
    }

    private [bootstrap] object Source {
      def single[A](a: A): Source[A, _] =
        akka.stream.scaladsl.Source.single(a)
    }
  }
}
