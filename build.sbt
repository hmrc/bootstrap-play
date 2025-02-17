val scala2_13 = "2.13.16"
val scala3    = "3.3.5"

ThisBuild / majorVersion     := 9
ThisBuild / isPublicArtefact := true
ThisBuild / scalaVersion     := scala2_13
ThisBuild / scalacOptions    ++= Seq("-feature")

lazy val library = (project in file("."))
  .settings(publish / skip := true)
  .aggregate(
    bootstrapCommonPlay29, bootstrapTestPlay29, bootstrapBackendPlay29, bootstrapFrontendPlay29, bootstrapHealthPlay29,
    bootstrapCommonPlay30, bootstrapTestPlay30, bootstrapBackendPlay30, bootstrapFrontendPlay30, bootstrapHealthPlay30
  )

def copyPlay30Sources(module: Project) =
  CopySources.copySources(
    module,
    transformSource   = _.replace("org.apache.pekko", "akka"),
    transformResource = _.replace("pekko", "akka")
  )

lazy val bootstrapCommonPlay29 = Project("bootstrap-common-play-29", file("bootstrap-common-play-29"))
  .settings(
    crossScalaVersions := Seq(scala2_13),
    libraryDependencies ++= LibDependencies.common("play-29"),
    copyPlay30Sources(bootstrapCommonPlay30)
  )

lazy val bootstrapCommonPlay30 = Project("bootstrap-common-play-30", file("bootstrap-common-play-30"))
  .settings(
    crossScalaVersions := Seq(scala2_13, scala3),
    libraryDependencies ++= LibDependencies.common("play-30")
  )

lazy val bootstrapTestPlay29 = Project("bootstrap-test-play-29", file("bootstrap-test-play-29"))
  .settings(
    crossScalaVersions := Seq(scala2_13),
    libraryDependencies ++= LibDependencies.test("play-29"),
    copyPlay30Sources(bootstrapTestPlay30)
  )
  .dependsOn(bootstrapCommonPlay29)

lazy val bootstrapTestPlay30 = Project("bootstrap-test-play-30", file("bootstrap-test-play-30"))
  .settings(
    crossScalaVersions := Seq(scala2_13, scala3),
    libraryDependencies ++= LibDependencies.test("play-30")
  )
  .dependsOn(bootstrapCommonPlay30)

lazy val bootstrapBackendPlay29 = Project("bootstrap-backend-play-29", file("bootstrap-backend-play-29"))
  .settings(
    crossScalaVersions := Seq(scala2_13),
    libraryDependencies ++= LibDependencies.backend("play-29"),
    copyPlay30Sources(bootstrapBackendPlay30)
  ).dependsOn(
    bootstrapCommonPlay29,
    bootstrapTestPlay29 % "test->test",
    bootstrapHealthPlay29 // dependency just to add to classpath
  )

lazy val bootstrapBackendPlay30 = Project("bootstrap-backend-play-30", file("bootstrap-backend-play-30"))
  .settings(
    crossScalaVersions := Seq(scala2_13, scala3),
    libraryDependencies ++= LibDependencies.backend("play-30")
  ).dependsOn(
    bootstrapCommonPlay30,
    bootstrapTestPlay30 % "test->test",
    bootstrapHealthPlay30 // dependency just to add to classpath
  )

lazy val bootstrapFrontendPlay29 = Project("bootstrap-frontend-play-29", file("bootstrap-frontend-play-29"))
  .settings(
    crossScalaVersions := Seq(scala2_13),
    libraryDependencies ++= LibDependencies.frontend("play-29"),
    copyPlay30Sources(bootstrapFrontendPlay30)
  ).dependsOn(
    bootstrapCommonPlay29,
    bootstrapTestPlay29 % "test->test",
    bootstrapHealthPlay29 // dependency just to add to classpath
  )

lazy val bootstrapFrontendPlay30 = Project("bootstrap-frontend-play-30", file("bootstrap-frontend-play-30"))
  .settings(
    crossScalaVersions := Seq(scala2_13, scala3),
    libraryDependencies ++= LibDependencies.frontend("play-30")
  ).dependsOn(
    bootstrapCommonPlay30,
    bootstrapTestPlay30 % "test->test",
    bootstrapHealthPlay30 // dependency just to add to classpath
  )

lazy val bootstrapHealthPlay29 = Project("bootstrap-health-play-29", file("bootstrap-health-play-29"))
  .settings(
    crossScalaVersions := Seq(scala2_13),
    libraryDependencies ++= LibDependencies.health("play-29"),
    copyPlay30Sources(bootstrapHealthPlay30)
  )

lazy val bootstrapHealthPlay30 = Project("bootstrap-health-play-30", file("bootstrap-health-play-30"))
  .settings(
    crossScalaVersions := Seq(scala2_13, scala3),
    libraryDependencies ++= LibDependencies.health("play-30")
  )
