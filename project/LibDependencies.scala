import sbt._

object LibDependencies {

  private val play26Version  = "2.6.25"
  private val play27Version  = "2.7.9"
  private val play28Version  = "2.8.7"

  private val httpVerbsVersion = "13.8.0"

  val commonPlay26: Seq[ModuleID] = common(play26Version, "play-26")
  val commonPlay27: Seq[ModuleID] = common(play27Version, "play-27")
  val commonPlay28: Seq[ModuleID] = common(play28Version, "play-28")

  val frontendCommonPlay26: Seq[ModuleID] = frontendCommon(play26Version, "play-26")
  val frontendCommonPlay27: Seq[ModuleID] = frontendCommon(play27Version, "play-27")
  val frontendCommonPlay28: Seq[ModuleID] = frontendCommon(play28Version, "play-28")

  val testPlay26: Seq[ModuleID] = test(play26Version, "play-26")
  val testPlay27: Seq[ModuleID] = test(play27Version, "play-27")
  val testPlay28: Seq[ModuleID] = test(play28Version, "play-28")

  val healthPlay26: Seq[ModuleID] = health(play26Version, "play-26")
  val healthPlay27: Seq[ModuleID] = health(play27Version, "play-27")
  val healthPlay28: Seq[ModuleID] = health(play28Version, "play-28")

  private def common(playVersion: String, playSuffix: String) =
    Seq(
      "ch.qos.logback"          %  "logback-core"               % "1.2.3",
      "com.kenshoo"             %% "metrics-play"               % "2.7.3_0.8.2",
      "com.typesafe.play"       %% "play-guice"                 % playVersion,
      "io.dropwizard.metrics"   %  "metrics-graphite"           % "4.1.17",
      "uk.gov.hmrc"             %% "auth-client"                % s"5.6.0-$playSuffix",
      "uk.gov.hmrc"             %% "crypto"                     % "6.0.0",
      "uk.gov.hmrc"             %% s"http-verbs-$playSuffix"    % httpVerbsVersion,
      "uk.gov.hmrc"             %% s"play-auditing-$playSuffix" % "7.10.0",
      // the following are not used by bootstrap - but transitively added for clients
      "com.typesafe.play"       %% "filters-helpers"            % playVersion,
      "uk.gov.hmrc"             %% "logback-json-logger"        % "5.1.0",

      // test dependencies
      "com.github.tomakehurst"  %  "wiremock-jre8"              % "2.26.3"       % Test,
      "com.typesafe.play"       %% "play-test"                  % playVersion    % Test,
      "org.mockito"             %% "mockito-scala"              % "1.16.23"      % Test,
      "org.mockito"             %% "mockito-scala-scalatest"    % "1.16.23"      % Test,
      "com.vladsch.flexmark"    %  "flexmark-all"               % "0.35.10"      % Test,
      "org.scalacheck"          %% "scalacheck"                 % "1.15.2"       % Test,
      "org.scalatestplus"       %% "scalatestplus-mockito"      % "1.0.0-M2"     % Test,
      "org.scalatestplus.play"  %% "scalatestplus-play"         % scalaTestPlusPlayVerson(playVersion) % Test,
      "org.scalatestplus"       %% "scalatestplus-scalacheck"   % "3.1.0.0-RC2"  % Test
    )

  private def frontendCommon(playVersion: String, playSuffix: String) =
    common(playVersion, playSuffix) ++
      Seq(  "uk.gov.hmrc"       %% "play-allowlist-filter"      % s"1.0.0-$playSuffix") ++
      // test dependencies
      (if (playVersion == play28Version)
         Seq("com.typesafe.akka" %% "akka-stream-testkit"       % "2.6.10"       % Test)
       else Nil
      )

  private def test(playVersion: String, playSuffix: String) =
    Seq(
      "com.typesafe.play"       %% "play-test"                    % playVersion,
      "uk.gov.hmrc"             %% s"http-verbs-test-$playSuffix" % httpVerbsVersion,
      "org.scalatestplus.play"  %% "scalatestplus-play"           % scalaTestPlusPlayVerson(playVersion),
      testreport(playVersion),
      // we use the same version of scalatest across play versions for simplicity for internal testing
      // but most clients probably just want to use the one provided transitively by scalatestplus-play
      "org.scalatest"           %% "scalatest"                    % "3.2.3"       % Test,
      "com.vladsch.flexmark"    %  "flexmark-all"                 % "0.35.10"     % Test
    ) ++
      (if (playVersion == play28Version)
        Seq(
          "com.typesafe.akka"   %% "akka-stream-testkit"          % "2.6.10"      % Test,
          "com.typesafe.play"   %% "play-akka-http-server"        % "2.8.7"       % Test
        )
       else Nil
      )

  private def health(playVersion: String, playSuffix: String) =
    Seq(
      "com.typesafe.play"       %% "play"                       % playVersion,
      // test dependencies
      "org.scalatest"           %% "scalatest"                  % "3.2.3"        % Test,
      "com.vladsch.flexmark"    %  "flexmark-all"               % "0.35.10"      % Test,
      "org.scalatestplus.play"  %% "scalatestplus-play"         % scalaTestPlusPlayVerson(playVersion) % Test
    )

    private def scalaTestPlusPlayVerson(playVersion: String): String =
      if      (playVersion == play26Version) "3.1.3"
      else if (playVersion == play27Version) "4.0.3"
      else                                   "5.1.0"

    // provides the optional dependency of scalatest as pulled in by scalatestplus-play
    private def testreport(playVersion: String): ModuleID =
      if      (playVersion == play26Version) "org.pegdown"          % "pegdown"      %  "1.4.2"
      else if (playVersion == play27Version) "org.pegdown"          % "pegdown"      %  "1.4.2"
      else                                   "com.vladsch.flexmark" % "flexmark-all" % "0.35.10"
}
