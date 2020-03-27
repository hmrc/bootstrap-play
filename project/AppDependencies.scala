import sbt._

object AppDependencies {

  private val play26Version  = "2.6.24"
  private val play27Version  = "2.7.4"

  val compileCommonPlay26: Seq[ModuleID] = compileCommon(play26Version, "play-26")
  val compileCommonPlay27: Seq[ModuleID] = compileCommon(play27Version, "play-27")

  val testCommonPlay26: Seq[ModuleID] = testCommon(play26Version)
  val testCommonPlay27: Seq[ModuleID] = testCommon(play27Version)

  private def compileCommon(playVersion: String, playSuffix: String) = Seq(
    "ch.qos.logback"        % "logback-core"                % "1.1.7",
    "com.kenshoo"           %% "metrics-play"               % "2.6.6_0.6.2",
    "com.typesafe.play"     %% "filters-helpers"            % playVersion,
    "com.typesafe.play"     %% "play"                       % playVersion,
    "com.typesafe.play"     %% "play-guice"                 % playVersion,
    "com.typesafe.play"     %% "play-ahc-ws"                % playVersion,
    "io.dropwizard.metrics" % "metrics-graphite"            % "3.2.5",
    "uk.gov.hmrc"           %% "auth-client"                % s"2.33.0-$playSuffix",
    "uk.gov.hmrc"           %% "crypto"                     % "5.5.0",
    "uk.gov.hmrc"           %% "http-verbs"                 % s"10.6.0-$playSuffix",
    "uk.gov.hmrc"           %% "logback-json-logger"        % "4.6.0",
    "uk.gov.hmrc"           %% s"play-auditing-$playSuffix" % s"5.2.0",
    "uk.gov.hmrc"           %% "play-health"                % s"3.14.0-$playSuffix",
    "uk.gov.hmrc"           %% "time"                       % "3.6.0"
  )

  private def testCommon(playVersion: String) = Seq(
    "com.github.tomakehurst" % "wiremock-jre8"       % "2.21.0",
    "com.typesafe.play"      %% "play-test"          % play26Version,
    "org.mockito"            % "mockito-all"         % "1.9.5",
    "org.pegdown"            % "pegdown"             % "1.5.0",
    "org.scalacheck"         %% "scalacheck"         % "1.14.0",
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2"
  ).map(_ % Test)
}
