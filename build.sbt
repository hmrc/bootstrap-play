
import sbt.Keys._
import sbt._

val scala2_11 = "2.11.12"
val scala2_12 = "2.12.10"

lazy val commonSettings = Seq(
  organization := "uk.gov.hmrc",
  majorVersion := 2,
  scalaVersion := scala2_12,
  crossScalaVersions := Seq(scala2_11, scala2_12),
  makePublicallyAvailableOnBintray := true,
  resolvers := Seq(
                 Resolver.bintrayRepo("hmrc", "releases"),
                 Resolver.typesafeRepo("releases")
               ),
  scalacOptions ++= Seq("-deprecation")
)

lazy val library = (project in file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
  .settings(
    commonSettings,
    publish := {},
    publishAndDistribute := {}
  )
  .aggregate(
    bootstrapCommonPlay26, bootstrapTestPlay26, bootstrapBackendPlay26, bootstrapFrontendPlay26/*,
    bootstrapCommonPlay27, bootstrapTestPlay27, bootstrapBackendPlay27, bootstrapFrontendPlay27*/ // `play-health`, `auth-client` don't support play-27 yet
  )


lazy val bootstrapCommonPlay26 = Project("bootstrap-common-play-26", file("bootstrap-common-play-26"))
  .enablePlugins(SbtAutoBuildPlugin, SbtArtifactory)
  .settings(
    commonSettings,
    libraryDependencies ++= AppDependencies.compileCommonPlay26 ++ AppDependencies.testCommonPlay26
  )

lazy val bootstrapTestPlay26 = Project("bootstrap-test-play-26", file("bootstrap-test-play-26"))
  .enablePlugins(SbtAutoBuildPlugin, SbtArtifactory)
  .settings(
    commonSettings,
    libraryDependencies ++= AppDependencies.compileTestPlay26 ++ AppDependencies.testCommonPlay26
  ).dependsOn(bootstrapCommonPlay26)

lazy val bootstrapBackendPlay26 = Project("bootstrap-backend-play-26", file("bootstrap-backend-play-26"))
  .enablePlugins(SbtAutoBuildPlugin, SbtArtifactory)
  .settings(
    commonSettings
  ).dependsOn(
    bootstrapCommonPlay26,
    bootstrapTestPlay26 % "test->test"
  )

lazy val bootstrapFrontendPlay26 = Project("bootstrap-frontend-play-26", file("bootstrap-frontend-play-26"))
  .enablePlugins(SbtAutoBuildPlugin, SbtArtifactory)
  .settings(
    commonSettings
  ).dependsOn(
    bootstrapCommonPlay26,
    bootstrapTestPlay26 % "test->test"
  )

/*lazy val bootstrapCommonPlay27 = Project("bootstrap-common-play-27", file("bootstrap-common-play-27"))
  .enablePlugins(SbtAutoBuildPlugin, SbtArtifactory)
  .settings(
    commonSettings,
    libraryDependencies ++= AppDependencies.compileCommonPlay27 ++ AppDependencies.testCommonPlay27,
    Compile / scalaSource := (bootstrapCommonPlay26 / Compile / scalaSource).value,
    Test    / scalaSource := (bootstrapCommonPlay26 / Test    / scalaSource).value
  )

lazy val bootstrapTestPlay27 = Project("bootstrap-test-play-27", file("bootstrap-test-play-27"))
  .enablePlugins(SbtAutoBuildPlugin, SbtArtifactory)
  .settings(
    commonSettings,
    libraryDependencies ++= AppDependencies.testCommonPlay27,
    Compile / scalaSource := (bootstrapTestPlay26 / Compile / scalaSource).value,
    Test    / scalaSource := (bootstrapTestPlay26 / Test    / scalaSource).value
  ).dependsOn(bootstrapCommonPlay27)

lazy val bootstrapBackendPlay27 = Project("bootstrap-backend-play-27", file("bootstrap-backend-play-27"))
  .enablePlugins(SbtAutoBuildPlugin, SbtArtifactory)
  .settings(
    commonSettings,
    Compile / scalaSource := (bootstrapBackendPlay26 / Compile / scalaSource).value,
    Test    / scalaSource := (bootstrapBackendPlay26 / Test    / scalaSource).value
  ).dependsOn(bootstrapCommonPlay27)

lazy val bootstrapFrontendPlay27 = Project("bootstrap-frontend-play-27", file("bootstrap-frontend-play-27"))
  .enablePlugins(SbtAutoBuildPlugin, SbtArtifactory)
  .settings(
    commonSettings,
    Compile / scalaSource := (bootstrapFrontendPlay26 / Compile / scalaSource).value,
    Test    / scalaSource := (bootstrapFrontendPlay26 / Test    / scalaSource).value
  ).dependsOn(bootstrapCommonPlay27)
*/