
import sbt.Keys._
import sbt._

val scala2_11 = "2.11.12"
val scala2_12 = "2.12.10"

val crossScalaVersionsPlay26 = Seq(scala2_11, scala2_12)
val crossScalaVersionsPlay27 = Seq(scala2_12) // metrix not available for scala2_11

val silencerVersion = "1.4.4"

lazy val commonSettings = Seq(
  organization := "uk.gov.hmrc",
  majorVersion := 3,
  scalaVersion := scala2_12,
  makePublicallyAvailableOnBintray := true,
  scalacOptions ++= Seq("-feature"),
  libraryDependencies ++= Seq(
    compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
  )
)

lazy val library = (project in file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
  .settings(
    commonSettings,
    publish := {},
    publishAndDistribute := {}
  )
  .aggregate(
    bootstrapCommonPlay26, bootstrapTestPlay26, bootstrapBackendPlay26, bootstrapFrontendPlay26, bootstrapHealthPlay26,
    bootstrapCommonPlay27, bootstrapTestPlay27, bootstrapBackendPlay27, bootstrapFrontendPlay27, bootstrapHealthPlay27
  )

lazy val bootstrapCommonPlay26 = Project("bootstrap-common-play-26", file("bootstrap-common-play-26"))
  .enablePlugins(SbtAutoBuildPlugin, SbtArtifactory)
  .settings(
    commonSettings,
    crossScalaVersions := crossScalaVersionsPlay26,
    libraryDependencies ++= LibDependencies.commonPlay26
  )

lazy val bootstrapTestPlay26 = Project("bootstrap-test-play-26", file("bootstrap-test-play-26"))
  .enablePlugins(SbtAutoBuildPlugin, SbtArtifactory)
  .settings(
    commonSettings,
    crossScalaVersions := crossScalaVersionsPlay26,
    libraryDependencies ++= LibDependencies.testPlay26
  ).dependsOn(bootstrapCommonPlay26)

lazy val bootstrapBackendPlay26 = Project("bootstrap-backend-play-26", file("bootstrap-backend-play-26"))
  .enablePlugins(SbtAutoBuildPlugin, SbtArtifactory)
  .settings(
    commonSettings,
    crossScalaVersions := crossScalaVersionsPlay26,
    libraryDependencies ++= LibDependencies.commonPlay26
  ).dependsOn(
    bootstrapCommonPlay26,
    bootstrapTestPlay26 % "test->test",
    bootstrapHealthPlay26 // dependency just to add to classpath
  )

lazy val bootstrapFrontendPlay26 = Project("bootstrap-frontend-play-26", file("bootstrap-frontend-play-26"))
  .enablePlugins(SbtAutoBuildPlugin, SbtArtifactory)
  .settings(
    commonSettings,
    crossScalaVersions := crossScalaVersionsPlay26,
    libraryDependencies ++= LibDependencies.frontendCommonPlay26
  ).dependsOn(
    bootstrapCommonPlay26,
    bootstrapTestPlay26 % "test->test",
    bootstrapHealthPlay26 // dependency just to add to classpath
  )

lazy val bootstrapHealthPlay26 = Project("bootstrap-health-play-26", file("bootstrap-health-play-26"))
  .enablePlugins(SbtAutoBuildPlugin, SbtArtifactory)
  .settings(
    commonSettings,
    crossScalaVersions := crossScalaVersionsPlay26,
    libraryDependencies ++= LibDependencies.healthPlay26
  )

  def copySources(module: Project) = Seq(
    Compile / scalaSource := (module / Compile / scalaSource).value,
    Compile / resourceDirectory  := (module / Compile / resourceDirectory).value,
    Test    / scalaSource := (module / Test    / scalaSource).value,
    Test    / resourceDirectory   := (module / Test    / resourceDirectory).value
  )

lazy val bootstrapCommonPlay27 = Project("bootstrap-common-play-27", file("bootstrap-common-play-27"))
  .enablePlugins(SbtAutoBuildPlugin, SbtArtifactory)
  .settings(
    commonSettings,
    crossScalaVersions := crossScalaVersionsPlay27,
    libraryDependencies ++= LibDependencies.commonPlay27,
    copySources(bootstrapCommonPlay26)
  )

lazy val bootstrapTestPlay27 = Project("bootstrap-test-play-27", file("bootstrap-test-play-27"))
  .enablePlugins(SbtAutoBuildPlugin, SbtArtifactory)
  .settings(
    commonSettings,
    crossScalaVersions := crossScalaVersionsPlay27,
    libraryDependencies ++= LibDependencies.testPlay27,
    copySources(bootstrapTestPlay26)
  ).dependsOn(bootstrapCommonPlay27)

lazy val bootstrapBackendPlay27 = Project("bootstrap-backend-play-27", file("bootstrap-backend-play-27"))
  .enablePlugins(SbtAutoBuildPlugin, SbtArtifactory)
  .settings(
    commonSettings,
    crossScalaVersions := crossScalaVersionsPlay27,
    libraryDependencies ++= LibDependencies.commonPlay27,
    copySources(bootstrapBackendPlay26),
    unmanagedResourceDirectories in Compile += baseDirectory.value / "src/main/resources",
  ).dependsOn(
    bootstrapCommonPlay27,
    bootstrapTestPlay27 % "test->test",
    bootstrapHealthPlay27 // dependency just to add to classpath
  )

lazy val bootstrapFrontendPlay27 = Project("bootstrap-frontend-play-27", file("bootstrap-frontend-play-27"))
  .enablePlugins(SbtAutoBuildPlugin, SbtArtifactory)
  .settings(
    commonSettings,
    crossScalaVersions := crossScalaVersionsPlay27,
    libraryDependencies ++= LibDependencies.frontendCommonPlay27,
    copySources(bootstrapFrontendPlay26),
    unmanagedResourceDirectories in Compile += baseDirectory.value / "src/main/resources",
  ).dependsOn(
    bootstrapCommonPlay27,
    bootstrapTestPlay27 % "test->test",
    bootstrapHealthPlay27 // dependency just to add to classpath
  )

lazy val bootstrapHealthPlay27 = Project("bootstrap-health-play-27", file("bootstrap-health-play-27"))
  .enablePlugins(SbtAutoBuildPlugin, SbtArtifactory)
  .settings(
    commonSettings,
    crossScalaVersions := crossScalaVersionsPlay27,
    libraryDependencies ++= LibDependencies.healthPlay27,
    copySources(bootstrapHealthPlay26)
  )
