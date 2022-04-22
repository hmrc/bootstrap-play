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

package uk.gov.hmrc.play.bootstrap.data

import play.api.libs.Files.{SingletonTemporaryFileCreator, TemporaryFile}
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.FilePart

object MultipartGenerator {
  def aMultipartFileWithParams(params: (String, Seq[String])*): MultipartFormData[TemporaryFile] = {
    val file = SingletonTemporaryFileCreator.create("example-file.txt")
    val filePart = FilePart[TemporaryFile](key = "data", "example-file.txt", contentType = Some("text/plain"), ref = file)
    MultipartFormData[TemporaryFile](dataParts = params.toMap, files = Seq(filePart), badParts = Seq.empty)
  }

}
