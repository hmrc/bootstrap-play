val scala2_13 = "2.13.16"
val scala3    = "3.3.6"

ThisBuild / majorVersion     := 10
ThisBuild / isPublicArtefact := true
ThisBuild / scalaVersion     := scala2_13
ThisBuild / scalacOptions    ++= Seq("-feature")

lazy val library = (project in file("."))
  .settings(publish / skip := true)
  .aggregate(
    bootstrapCommonPlay30, bootstrapTestPlay30, bootstrapBackendPlay30, bootstrapFrontendPlay30, bootstrapHealthPlay30
  )

lazy val bootstrapCommonPlay30 = Project("bootstrap-common-play-30", file("bootstrap-common-play-30"))
  .settings(
    crossScalaVersions := Seq(scala2_13, scala3),
    libraryDependencies ++= LibDependencies.common("play-30")
  )

lazy val bootstrapTestPlay30 = Project("bootstrap-test-play-30", file("bootstrap-test-play-30"))
  .settings(
    crossScalaVersions := Seq(scala2_13, scala3),
    libraryDependencies ++= LibDependencies.test("play-30")
  )
  .dependsOn(bootstrapCommonPlay30)

lazy val bootstrapBackendPlay30 = Project("bootstrap-backend-play-30", file("bootstrap-backend-play-30"))
  .settings(
    crossScalaVersions := Seq(scala2_13, scala3),
    libraryDependencies ++= LibDependencies.backend("play-30")
  ).dependsOn(
    bootstrapCommonPlay30,
    bootstrapTestPlay30 % "test->test",
    bootstrapHealthPlay30 // dependency just to add to classpath
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

lazy val bootstrapHealthPlay30 = Project("bootstrap-health-play-30", file("bootstrap-health-play-30"))
  .settings(
    crossScalaVersions := Seq(scala2_13, scala3),
    libraryDependencies ++= LibDependencies.health("play-30")
  )
