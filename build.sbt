val scala2_12 = "2.12.18"
val scala2_13 = "2.13.12"

ThisBuild / majorVersion     := 7
ThisBuild / isPublicArtefact := true
ThisBuild / scalaVersion     := scala2_13
ThisBuild / scalacOptions    ++= Seq("-feature")

lazy val library = (project in file("."))
  .settings(publish / skip := true)
  .aggregate(
    bootstrapCommonPlay28, bootstrapTestPlay28, bootstrapBackendPlay28, bootstrapFrontendPlay28, bootstrapHealthPlay28,
    bootstrapCommonPlay29, bootstrapTestPlay29, bootstrapBackendPlay29, bootstrapFrontendPlay29, bootstrapHealthPlay29,
    bootstrapCommonPlay30, bootstrapTestPlay30, bootstrapBackendPlay30, bootstrapFrontendPlay30, bootstrapHealthPlay30
  )

def copySources(module: Project) = Seq(
  Compile / scalaSource       := (module / Compile / scalaSource      ).value,
  Compile / resourceDirectory := (module / Compile / resourceDirectory).value,
  Test    / scalaSource       := (module / Test    / scalaSource      ).value,
  Test    / resourceDirectory := (module / Test    / resourceDirectory).value
)

/** copies source files from one module to another, and applies transformation */
def copySources(module: Project, transform: String => String) = Seq(
  Compile / sourceGenerators   += transformWith(module / Compile / scalaSource      , Compile / sourceManaged  , transform).taskValue,
  Compile / resourceGenerators += transformWith(module / Compile / resourceDirectory, Compile / resourceManaged, transform).taskValue,
  Test    / sourceGenerators   += transformWith(module / Test    / scalaSource      , Test    / sourceManaged  , transform).taskValue,
  Test    / resourceGenerators += transformWith(module / Test    / resourceDirectory, Test    / resourceManaged, transform).taskValue,
  // generated sources are not included in source.jar by default
  Compile / packageSrc / mappings ++= {
    def include(location: File) = {
      val files = (location ** "*").get.filterNot(_.isDirectory)
      files.map { file =>
        file -> file.getPath.stripPrefix(location.getPath)
      }
    }
    include((Compile / sourceManaged).value) ++
      include((Compile / resourceManaged).value)
  }
)

def transformWith(sourceSetting: SettingKey[File], targetSourceSetting: SettingKey[File], transform: String => String) =
  Def.task {
    val source       = sourceSetting.value
    val targetSource = targetSourceSetting.value
    val files        = (source ** "*").get.filterNot(_.isDirectory)
    println(s"Copying and transforming the following files for ${moduleName.value} scalaVersion ${scalaVersion.value}: files:\n${files.mkString("\n")}}")
    files.map { file =>
      val targetFile = new java.io.File(file.getParent.replace(source.getPath, targetSource.getPath)) / file.getName
      IO.write(targetFile, transform(IO.read(file)))
      targetFile
    }
  }

lazy val bootstrapCommonPlay28 = Project("bootstrap-common-play-28", file("bootstrap-common-play-28"))
  .settings(
    crossScalaVersions := Seq(scala2_12, scala2_13),
    libraryDependencies ++= LibDependencies.commonPlay28,
    copySources(bootstrapCommonPlay30, _.replace("org.apache.pekko", "akka"))
  )

lazy val bootstrapCommonPlay29 = Project("bootstrap-common-play-29", file("bootstrap-common-play-29"))
  .settings(
    crossScalaVersions := Seq(scala2_13),
    libraryDependencies ++= LibDependencies.commonPlay29,
    copySources(bootstrapCommonPlay30, _.replace("org.apache.pekko", "akka"))
  )

lazy val bootstrapCommonPlay30 = Project("bootstrap-common-play-30", file("bootstrap-common-play-30"))
  .settings(
    crossScalaVersions := Seq(scala2_13),
    libraryDependencies ++= LibDependencies.commonPlay30
  )

lazy val bootstrapTestPlay28 = Project("bootstrap-test-play-28", file("bootstrap-test-play-28"))
  .settings(
    libraryDependencies ++= LibDependencies.testPlay28,
    copySources(bootstrapTestPlay30, _.replace("org.apache.pekko", "akka"))
  )
  .dependsOn(bootstrapCommonPlay28)

lazy val bootstrapTestPlay29 = Project("bootstrap-test-play-29", file("bootstrap-test-play-29"))
  .settings(
    libraryDependencies ++= LibDependencies.testPlay29,
    copySources(bootstrapTestPlay30, _.replace("org.apache.pekko", "akka"))
  )
  .dependsOn(bootstrapCommonPlay29)

lazy val bootstrapTestPlay30 = Project("bootstrap-test-play-30", file("bootstrap-test-play-30"))
  .settings(
    libraryDependencies ++= LibDependencies.testPlay30
  )
  .dependsOn(bootstrapCommonPlay30)

lazy val bootstrapBackendPlay28 = Project("bootstrap-backend-play-28", file("bootstrap-backend-play-28"))
  .settings(
    libraryDependencies ++= LibDependencies.commonPlay28,
    copySources(bootstrapBackendPlay30, _.replace("org.apache.pekko", "akka")
                                         .replace("pekko", "akka") // for backend.conf
    )
  ).dependsOn(
    bootstrapCommonPlay28,
    bootstrapTestPlay28 % "test->test",
    bootstrapHealthPlay28 // dependency just to add to classpath
  )

lazy val bootstrapBackendPlay29 = Project("bootstrap-backend-play-29", file("bootstrap-backend-play-29"))
  .settings(
    libraryDependencies ++= LibDependencies.commonPlay29,
    copySources(bootstrapBackendPlay30, _.replace("org.apache.pekko", "akka")
                                         .replace("pekko", "akka") // for backend.conf
    )
  ).dependsOn(
    bootstrapCommonPlay29,
    bootstrapTestPlay29 % "test->test",
    bootstrapHealthPlay29 // dependency just to add to classpath
  )

lazy val bootstrapBackendPlay30 = Project("bootstrap-backend-play-30", file("bootstrap-backend-play-30"))
  .settings(
    libraryDependencies ++= LibDependencies.commonPlay30
  ).dependsOn(
    bootstrapCommonPlay30,
    bootstrapTestPlay30 % "test->test",
    bootstrapHealthPlay30 // dependency just to add to classpath
  )

lazy val bootstrapFrontendPlay28 = Project("bootstrap-frontend-play-28", file("bootstrap-frontend-play-28"))
  .settings(
    libraryDependencies ++= LibDependencies.frontendCommonPlay28,
    copySources(bootstrapFrontendPlay30, _.replace("org.apache.pekko", "akka")
                                          .replace("pekko", "akka") // for frontend.conf
    )
  ).dependsOn(
    bootstrapCommonPlay28,
    bootstrapTestPlay28 % "test->test",
    bootstrapHealthPlay28 // dependency just to add to classpath
  )

lazy val bootstrapFrontendPlay29 = Project("bootstrap-frontend-play-29", file("bootstrap-frontend-play-29"))
  .settings(
    libraryDependencies ++= LibDependencies.frontendCommonPlay29,
    copySources(bootstrapFrontendPlay30, _.replace("org.apache.pekko", "akka")
                                          .replace("pekko", "akka") // for frontend.conf
    )
  ).dependsOn(
    bootstrapCommonPlay29,
    bootstrapTestPlay29 % "test->test",
    bootstrapHealthPlay29 // dependency just to add to classpath
  )

lazy val bootstrapFrontendPlay30 = Project("bootstrap-frontend-play-30", file("bootstrap-frontend-play-30"))
  .settings(
    libraryDependencies ++= LibDependencies.frontendCommonPlay30
  ).dependsOn(
    bootstrapCommonPlay30,
    bootstrapTestPlay30 % "test->test",
    bootstrapHealthPlay30 // dependency just to add to classpath
  )

lazy val bootstrapHealthPlay28 = Project("bootstrap-health-play-28", file("bootstrap-health-play-28"))
  .settings(
    libraryDependencies ++= LibDependencies.healthPlay28,
    copySources(bootstrapHealthPlay30)
  )

lazy val bootstrapHealthPlay29 = Project("bootstrap-health-play-29", file("bootstrap-health-play-29"))
  .settings(
    libraryDependencies ++= LibDependencies.healthPlay29,
    copySources(bootstrapHealthPlay30)
  )

lazy val bootstrapHealthPlay30 = Project("bootstrap-health-play-30", file("bootstrap-health-play-30"))
  .settings(
    libraryDependencies ++= LibDependencies.healthPlay30
  )
