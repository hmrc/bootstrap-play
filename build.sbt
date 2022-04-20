import sbt.Keys._
import sbt._

val silencerVersion = "1.7.8"

val scala2_12 = "2.12.15"
val scala2_13 = "2.13.8"

lazy val commonSettings = Seq(
  organization := "uk.gov.hmrc",
  majorVersion := 5,
  isPublicArtefact := true,
  scalaVersion := scala2_12,
  crossScalaVersions := Seq(scala2_12, scala2_13),
  scalacOptions ++= Seq("-feature"),
  libraryDependencies ++= Seq(
    compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
  )
)

lazy val library = (project in file("."))
  .settings(
    commonSettings,
    publish / skip := true
  )
  .aggregate(
    bootstrapCommonPlay28, bootstrapTestPlay28, bootstrapBackendPlay28, bootstrapFrontendPlay28, bootstrapHealthPlay28
  )

lazy val bootstrapCommonPlay28 = Project("bootstrap-common-play-28", file("bootstrap-common-play-28"))
  .settings(
    commonSettings,
    libraryDependencies ++= LibDependencies.commonPlay28,
    Compile / unmanagedSourceDirectories   += baseDirectory.value / "../bootstrap-common/src/main/scala",
    Compile / unmanagedResourceDirectories += baseDirectory.value / "../bootstrap-common/src/main/resources",
    Test    / unmanagedSourceDirectories   += baseDirectory.value / "../bootstrap-common/src/test/scala",
    Test    / unmanagedResourceDirectories += baseDirectory.value / "../bootstrap-common/src/test/resources"
  )

lazy val bootstrapTestPlay28 = Project("bootstrap-test-play-28", file("bootstrap-test-play-28"))
  .settings(
    commonSettings,
    libraryDependencies ++= LibDependencies.testPlay28
  )
  .dependsOn(bootstrapCommonPlay28)

lazy val bootstrapBackendPlay28 = Project("bootstrap-backend-play-28", file("bootstrap-backend-play-28"))
  .settings(
    commonSettings,
    libraryDependencies ++= LibDependencies.commonPlay28
  ).dependsOn(
    bootstrapCommonPlay28,
    bootstrapTestPlay28 % "test->test",
    bootstrapHealthPlay28 // dependency just to add to classpath
  )

lazy val bootstrapFrontendPlay28 = Project("bootstrap-frontend-play-28", file("bootstrap-frontend-play-28"))
  .settings(
    commonSettings,
    libraryDependencies ++= LibDependencies.frontendCommonPlay28
  ).dependsOn(
    bootstrapCommonPlay28,
    bootstrapTestPlay28 % "test->test",
    bootstrapHealthPlay28 // dependency just to add to classpath
  )

lazy val bootstrapHealthPlay28 = Project("bootstrap-health-play-28", file("bootstrap-health-play-28"))
  .settings(
    commonSettings,
    libraryDependencies ++= LibDependencies.healthPlay28
  )
