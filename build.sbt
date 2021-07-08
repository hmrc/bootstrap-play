import sbt.Keys._
import sbt._

val silencerVersion = "1.7.5"

lazy val commonSettings = Seq(
  organization := "uk.gov.hmrc",
  majorVersion := 5,
  scalaVersion := "2.12.14",
  isPublicArtefact := true,
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
    bootstrapCommonPlay26, bootstrapTestPlay26, bootstrapBackendPlay26, bootstrapFrontendPlay26, bootstrapHealthPlay26,
    bootstrapCommonPlay27, bootstrapTestPlay27, bootstrapBackendPlay27, bootstrapFrontendPlay27, bootstrapHealthPlay27,
    bootstrapCommonPlay28, bootstrapTestPlay28, bootstrapBackendPlay28, bootstrapFrontendPlay28, bootstrapHealthPlay28,
  )

lazy val bootstrapCommonPlay26 = Project("bootstrap-common-play-26", file("bootstrap-common-play-26"))
  .settings(
    commonSettings,
    libraryDependencies ++= LibDependencies.commonPlay26,
    Compile / unmanagedSourceDirectories   += baseDirectory.value / "../bootstrap-common/src/main/scala",
    Compile / unmanagedResourceDirectories += baseDirectory.value / "../bootstrap-common/src/main/resources",
    Test    / unmanagedSourceDirectories   += baseDirectory.value / "../bootstrap-common/src/test/scala",
    Test    / unmanagedResourceDirectories += baseDirectory.value / "../bootstrap-common/src/test/resources"
  )

lazy val bootstrapTestPlay26 = Project("bootstrap-test-play-26", file("bootstrap-test-play-26"))
  .settings(
    commonSettings,
    libraryDependencies ++= LibDependencies.testPlay26
  ).dependsOn(bootstrapCommonPlay26)

lazy val bootstrapBackendPlay26 = Project("bootstrap-backend-play-26", file("bootstrap-backend-play-26"))
  .settings(
    commonSettings,
    libraryDependencies ++= LibDependencies.commonPlay26
  ).dependsOn(
    bootstrapCommonPlay26,
    bootstrapTestPlay26 % "test->test",
    bootstrapHealthPlay26 // dependency just to add to classpath
  )

lazy val bootstrapFrontendPlay26 = Project("bootstrap-frontend-play-26", file("bootstrap-frontend-play-26"))
  .settings(
    commonSettings,
    libraryDependencies ++= LibDependencies.frontendCommonPlay26
  ).dependsOn(
    bootstrapCommonPlay26,
    bootstrapTestPlay26 % "test->test",
    bootstrapHealthPlay26 // dependency just to add to classpath
  )

lazy val bootstrapHealthPlay26 = Project("bootstrap-health-play-26", file("bootstrap-health-play-26"))
  .settings(
    commonSettings,
    libraryDependencies ++= LibDependencies.healthPlay26
  )

  def copySources(module: Project) = Seq(
    Compile / scalaSource       := (module / Compile / scalaSource      ).value,
    Compile / resourceDirectory := (module / Compile / resourceDirectory).value,
    Test    / scalaSource       := (module / Test    / scalaSource      ).value,
    Test    / resourceDirectory := (module / Test    / resourceDirectory).value
  )

lazy val bootstrapCommonPlay27 = Project("bootstrap-common-play-27", file("bootstrap-common-play-27"))
  .settings(
    commonSettings,
    libraryDependencies ++= LibDependencies.commonPlay27,
    Compile / unmanagedSourceDirectories   += baseDirectory.value / "../bootstrap-common/src/main/scala",
    Compile / unmanagedResourceDirectories += baseDirectory.value / "../bootstrap-common/src/main/resources",
    Test    / unmanagedSourceDirectories   += baseDirectory.value / "../bootstrap-common/src/test/scala",
    Test    / unmanagedResourceDirectories += baseDirectory.value / "../bootstrap-common/src/test/resources"
  )

lazy val bootstrapTestPlay27 = Project("bootstrap-test-play-27", file("bootstrap-test-play-27"))
  .settings(
    commonSettings,
    libraryDependencies ++= LibDependencies.testPlay27,
    copySources(bootstrapTestPlay26)
  ).dependsOn(bootstrapCommonPlay27)

lazy val bootstrapBackendPlay27 = Project("bootstrap-backend-play-27", file("bootstrap-backend-play-27"))
  .settings(
    commonSettings,
    libraryDependencies ++= LibDependencies.commonPlay27,
    copySources(bootstrapBackendPlay26)
  ).dependsOn(
    bootstrapCommonPlay27,
    bootstrapTestPlay27 % "test->test",
    bootstrapHealthPlay27 // dependency just to add to classpath
  )

lazy val bootstrapFrontendPlay27 = Project("bootstrap-frontend-play-27", file("bootstrap-frontend-play-27"))
  .settings(
    commonSettings,
    libraryDependencies ++= LibDependencies.frontendCommonPlay27,
    copySources(bootstrapFrontendPlay26)
  ).dependsOn(
    bootstrapCommonPlay27,
    bootstrapTestPlay27 % "test->test",
    bootstrapHealthPlay27 // dependency just to add to classpath
  )

lazy val bootstrapHealthPlay27 = Project("bootstrap-health-play-27", file("bootstrap-health-play-27"))
  .settings(
    commonSettings,
    libraryDependencies ++= LibDependencies.healthPlay27,
    copySources(bootstrapHealthPlay26)
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
    libraryDependencies ++= LibDependencies.commonPlay28,
    copySources(bootstrapBackendPlay26)
  ).dependsOn(
    bootstrapCommonPlay28,
    bootstrapTestPlay28 % "test->test",
    bootstrapHealthPlay28 // dependency just to add to classpath
  )

lazy val bootstrapFrontendPlay28 = Project("bootstrap-frontend-play-28", file("bootstrap-frontend-play-28"))
  .settings(
    commonSettings,
    libraryDependencies ++= LibDependencies.frontendCommonPlay28,
    copySources(bootstrapFrontendPlay26)
  ).dependsOn(
    bootstrapCommonPlay28,
    bootstrapTestPlay28 % "test->test",
    bootstrapHealthPlay28 // dependency just to add to classpath
  )

lazy val bootstrapHealthPlay28 = Project("bootstrap-health-play-28", file("bootstrap-health-play-28"))
  .settings(
    commonSettings,
    libraryDependencies ++= LibDependencies.healthPlay28,
    copySources(bootstrapHealthPlay26)
  )
