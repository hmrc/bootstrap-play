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
    bootstrapCommonPlay29, bootstrapTestPlay29, bootstrapBackendPlay29, bootstrapFrontendPlay29, bootstrapHealthPlay29
  )

def shareSources(location: String) = Seq(
  Compile / unmanagedSourceDirectories   += baseDirectory.value / s"../$location/src/main/scala",
  Compile / unmanagedResourceDirectories += baseDirectory.value / s"../$location/src/main/resources",
  Test    / unmanagedSourceDirectories   += baseDirectory.value / s"../$location/src/test/scala",
  Test    / unmanagedResourceDirectories += baseDirectory.value / s"../$location/src/test/resources"
)

lazy val bootstrapCommonPlay28 = Project("bootstrap-common-play-28", file("bootstrap-common-play-28"))
  .settings(
    crossScalaVersions := Seq(scala2_12, scala2_13),
    libraryDependencies ++= LibDependencies.commonPlay28,
    shareSources("bootstrap-common")
  )

lazy val bootstrapCommonPlay29 = Project("bootstrap-common-play-29", file("bootstrap-common-play-29"))
  .settings(
    crossScalaVersions := Seq(scala2_13),
    libraryDependencies ++= LibDependencies.commonPlay29,
    shareSources("bootstrap-common")
  )

lazy val bootstrapTestPlay28 = Project("bootstrap-test-play-28", file("bootstrap-test-play-28"))
  .settings(
    libraryDependencies ++= LibDependencies.testPlay28,
    shareSources("bootstrap-test")
  )
  .dependsOn(bootstrapCommonPlay28)

lazy val bootstrapTestPlay29 = Project("bootstrap-test-play-29", file("bootstrap-test-play-29"))
  .settings(
    libraryDependencies ++= LibDependencies.testPlay29,
    shareSources("bootstrap-test")
  )
  .dependsOn(bootstrapCommonPlay29)

lazy val bootstrapBackendPlay28 = Project("bootstrap-backend-play-28", file("bootstrap-backend-play-28"))
  .settings(
    libraryDependencies ++= LibDependencies.commonPlay28,
    shareSources("bootstrap-backend")
  ).dependsOn(
    bootstrapCommonPlay28,
    bootstrapTestPlay28 % "test->test",
    bootstrapHealthPlay28 // dependency just to add to classpath
  )

lazy val bootstrapBackendPlay29 = Project("bootstrap-backend-play-29", file("bootstrap-backend-play-29"))
  .settings(
    libraryDependencies ++= LibDependencies.commonPlay29,
    shareSources("bootstrap-backend")
  ).dependsOn(
    bootstrapCommonPlay29,
    bootstrapTestPlay29 % "test->test",
    bootstrapHealthPlay29 // dependency just to add to classpath
  )

lazy val bootstrapFrontendPlay28 = Project("bootstrap-frontend-play-28", file("bootstrap-frontend-play-28"))
  .settings(
    libraryDependencies ++= LibDependencies.frontendCommonPlay28,
    shareSources("bootstrap-frontend")
  ).dependsOn(
    bootstrapCommonPlay28,
    bootstrapTestPlay28 % "test->test",
    bootstrapHealthPlay28 // dependency just to add to classpath
  )

lazy val bootstrapFrontendPlay29 = Project("bootstrap-frontend-play-29", file("bootstrap-frontend-play-29"))
  .settings(
    libraryDependencies ++= LibDependencies.frontendCommonPlay29,
    shareSources("bootstrap-frontend")
  ).dependsOn(
    bootstrapCommonPlay29,
    bootstrapTestPlay29 % "test->test",
    bootstrapHealthPlay29 // dependency just to add to classpath
  )

lazy val bootstrapHealthPlay28 = Project("bootstrap-health-play-28", file("bootstrap-health-play-28"))
  .settings(
    libraryDependencies ++= LibDependencies.healthPlay28,
    shareSources("bootstrap-health")
  )

lazy val bootstrapHealthPlay29 = Project("bootstrap-health-play-29", file("bootstrap-health-play-29"))
  .settings(
    libraryDependencies ++= LibDependencies.healthPlay29,
    shareSources("bootstrap-health")
  )
