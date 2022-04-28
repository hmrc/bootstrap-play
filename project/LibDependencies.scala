import sbt._

object LibDependencies {

  private val play28Version    = "2.8.15"
  private val httpVerbsVersion = "14.1.0"
  private val akkaVersion      = "2.6.19"

  val commonPlay28: Seq[ModuleID] = common(play28Version, "play-28")

  val frontendCommonPlay28: Seq[ModuleID] = frontendCommon(play28Version, "play-28")

  val testPlay28: Seq[ModuleID] = test(play28Version, "play-28")

  val healthPlay28: Seq[ModuleID] = health(play28Version, "play-28")

  private def common(playVersion: String, playSuffix: String) =
    Seq(
      "ch.qos.logback"          %  "logback-core"               % "1.2.3",
      "com.kenshoo"             %% "metrics-play"               % "2.7.3_0.8.2", // this is compatible with play 2.8
      "com.typesafe.play"       %% "play-guice"                 % playVersion,
      "io.dropwizard.metrics"   %  "metrics-graphite"           % "4.1.17",
      "uk.gov.hmrc"             %% "auth-client"                % s"5.12.0-$playSuffix",
      "uk.gov.hmrc"             %% "crypto"                     % "6.1.0",
      "uk.gov.hmrc"             %% s"http-verbs-$playSuffix"    % httpVerbsVersion,
      "uk.gov.hmrc"             %% s"play-auditing-$playSuffix" % "8.2.0",
      // the following are not used by bootstrap - but transitively added for clients
      "com.typesafe.play"       %% "filters-helpers"            % playVersion,
      "uk.gov.hmrc"             %% "logback-json-logger"        % "5.2.0",

      // test dependencies
      "com.github.tomakehurst"  %  "wiremock-jre8"              % "2.26.3"       % Test,
      "com.typesafe.play"       %% "play-test"                  % playVersion    % Test,
      "org.mockito"             %% "mockito-scala"              % "1.16.23"      % Test,
      "org.mockito"             %% "mockito-scala-scalatest"    % "1.16.23"      % Test,
      "com.vladsch.flexmark"    %  "flexmark-all"               % "0.35.10"      % Test,
      "org.scalacheck"          %% "scalacheck"                 % "1.15.2"       % Test,
      "org.mockito"             %% "mockito-scala-scalatest"    % "1.16.49"      % Test,
      "org.scalatestplus.play"  %% "scalatestplus-play"         % scalaTestPlusPlayVersion(playVersion) % Test,
      "org.scalatestplus"       %% "scalatestplus-scalacheck"   % "3.1.0.0-RC2"  % Test
    )

  private def frontendCommon(playVersion: String, playSuffix: String) =
    common(playVersion, playSuffix) ++
      Seq(
        "uk.gov.hmrc"       %% s"play-allowlist-filter-$playSuffix" % "1.1.0",
        "com.typesafe.akka" %% "akka-stream-testkit"                % akkaVersion % Test
      )

  private def test(playVersion: String, playSuffix: String) =
    Seq(
      "com.typesafe.play"       %% "play-test"                    % playVersion,
      "uk.gov.hmrc"             %% s"http-verbs-test-$playSuffix" % httpVerbsVersion,
      "org.scalatestplus.play"  %% "scalatestplus-play"           % scalaTestPlusPlayVersion(playVersion),
      testReport(playVersion),
      // we use the same version of scalatest across play versions for simplicity for internal testing
      // but most clients probably just want to use the one provided transitively by scalatestplus-play
      "org.scalatest"           %% "scalatest"                    % "3.2.3"       % Test,
      "com.typesafe.akka"       %% "akka-stream-testkit"          % akkaVersion   % Test,
      "com.typesafe.play"       %% "play-akka-http-server"        % playVersion   % Test
    )

  private def health(playVersion: String, playSuffix: String) =
    Seq(
      "com.typesafe.play"       %% "play"                       % playVersion,
      // test dependencies
      "org.scalatest"           %% "scalatest"                  % "3.2.3"        % Test,
      "com.vladsch.flexmark"    %  "flexmark-all"               % "0.35.10"      % Test,
      "org.scalatestplus.play"  %% "scalatestplus-play"         % scalaTestPlusPlayVersion(playVersion) % Test
    )

    private def scalaTestPlusPlayVersion(playVersion: String): String =
      if (playVersion == play28Version) "5.1.0"
      else sys.error("Unsupported playVersion")

    // provides the optional dependency of scalatest as pulled in by scalatestplus-play
    private def testReport(playVersion: String): ModuleID =
      if (playVersion == play28Version) "com.vladsch.flexmark" % "flexmark-all" % "0.35.10"
      else sys.error("Unsupported playVersion")
}
