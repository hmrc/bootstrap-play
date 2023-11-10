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

/** Copies source files from one module to another, and applies transformations */
def copySources(module: Project, transformSource: String => String, transformResource: String => String) = {
  def transformWith(fromSetting: SettingKey[File], toSetting: SettingKey[File], transform: String => String) =
    Def.task {
      val from  = fromSetting.value
      val to    = toSetting.value
      val files = (from ** "*").get.filterNot(_.isDirectory)
      println(s"Copying and transforming the following files for ${moduleName.value} scalaVersion ${scalaVersion.value}: files:\n${files.mkString("\n")}}")
      files.map { file =>
        val targetFile = new java.io.File(file.getParent.replace(from.getPath, to.getPath)) / file.getName
        IO.write(targetFile, transform(IO.read(file)))
        targetFile
      }
    }

  def include(location: File) = {
    val files = (location ** "*").get.filterNot(_.isDirectory)
    files.map(file => file -> file.getPath.stripPrefix(location.getPath))
  }

  Seq(
    Compile / sourceGenerators   += transformWith(module / Compile / scalaSource      , Compile / sourceManaged  , transformSource  ).taskValue,
    Compile / resourceGenerators += transformWith(module / Compile / resourceDirectory, Compile / resourceManaged, transformResource).taskValue,
    Test    / sourceGenerators   += transformWith(module / Test    / scalaSource      , Test    / sourceManaged  , transformSource  ).taskValue,
    Test    / resourceGenerators += transformWith(module / Test    / resourceDirectory, Test    / resourceManaged, transformResource).taskValue,
    // generated sources are not included in source.jar by default
    Compile / packageSrc / mappings ++= include((Compile / sourceManaged).value) ++
                                          include((Compile / resourceManaged).value)
  )
}

def copyPlay30Sources(module: Project) =
  copySources(
    module,
    transformSource   = _.replace("org.apache.pekko", "akka"),
    transformResource = _.replace("pekko", "akka")
  )

lazy val bootstrapCommonPlay28 = Project("bootstrap-common-play-28", file("bootstrap-common-play-28"))
  .settings(
    crossScalaVersions := Seq(scala2_12, scala2_13),
    libraryDependencies ++= LibDependencies.commonPlay28,
    copyPlay30Sources(bootstrapCommonPlay30)
  )

lazy val bootstrapCommonPlay29 = Project("bootstrap-common-play-29", file("bootstrap-common-play-29"))
  .settings(
    crossScalaVersions := Seq(scala2_13),
    libraryDependencies ++= LibDependencies.commonPlay29,
    copyPlay30Sources(bootstrapCommonPlay30)
  )

lazy val bootstrapCommonPlay30 = Project("bootstrap-common-play-30", file("bootstrap-common-play-30"))
  .settings(
    crossScalaVersions := Seq(scala2_13),
    libraryDependencies ++= LibDependencies.commonPlay30
  )

lazy val bootstrapTestPlay28 = Project("bootstrap-test-play-28", file("bootstrap-test-play-28"))
  .settings(
    libraryDependencies ++= LibDependencies.testPlay28,
    copyPlay30Sources(bootstrapTestPlay30)
  )
  .dependsOn(bootstrapCommonPlay28)

lazy val bootstrapTestPlay29 = Project("bootstrap-test-play-29", file("bootstrap-test-play-29"))
  .settings(
    libraryDependencies ++= LibDependencies.testPlay29,
    copyPlay30Sources(bootstrapTestPlay30)
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
    copyPlay30Sources(bootstrapBackendPlay30)
  ).dependsOn(
    bootstrapCommonPlay28,
    bootstrapTestPlay28 % "test->test",
    bootstrapHealthPlay28 // dependency just to add to classpath
  )

lazy val bootstrapBackendPlay29 = Project("bootstrap-backend-play-29", file("bootstrap-backend-play-29"))
  .settings(
    libraryDependencies ++= LibDependencies.commonPlay29,
    copyPlay30Sources(bootstrapBackendPlay30)
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
    copyPlay30Sources(bootstrapFrontendPlay30)
  ).dependsOn(
    bootstrapCommonPlay28,
    bootstrapTestPlay28 % "test->test",
    bootstrapHealthPlay28 // dependency just to add to classpath
  )

lazy val bootstrapFrontendPlay29 = Project("bootstrap-frontend-play-29", file("bootstrap-frontend-play-29"))
  .settings(
    libraryDependencies ++= LibDependencies.frontendCommonPlay29,
    copyPlay30Sources(bootstrapFrontendPlay30)
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
    copyPlay30Sources(bootstrapHealthPlay30)
  )

lazy val bootstrapHealthPlay29 = Project("bootstrap-health-play-29", file("bootstrap-health-play-29"))
  .settings(
    libraryDependencies ++= LibDependencies.healthPlay29,
    copyPlay30Sources(bootstrapHealthPlay30)
  )

lazy val bootstrapHealthPlay30 = Project("bootstrap-health-play-30", file("bootstrap-health-play-30"))
  .settings(
    libraryDependencies ++= LibDependencies.healthPlay30
  )
