import sbt._

object LibDependencies {

  private val play28Version          = "2.8.20"
  private val play29Version          = "2.9.0"
  private val httpVerbsVersion       = "14.11.0-SNAPSHOT"
  private val akkaVersion            = "2.6.21"
  private val jacksonVersion         = "2.12.7"
  private val jacksonDatabindVersion = "2.12.7.1"

  val commonPlay28: Seq[ModuleID] = common(play28Version, "play-28")
  val commonPlay29: Seq[ModuleID] = common(play29Version, "play-29")

  val frontendCommonPlay28: Seq[ModuleID] = frontendCommon(play28Version, "play-28")
  val frontendCommonPlay29: Seq[ModuleID] = frontendCommon(play29Version, "play-29")

  val testPlay28: Seq[ModuleID] = test(play28Version, "play-28")
  val testPlay29: Seq[ModuleID] = test(play29Version, "play-29")

  val healthPlay28: Seq[ModuleID] = health(play28Version, "play-28")
  val healthPlay29: Seq[ModuleID] = health(play29Version, "play-29")

  private def common(playVersion: String, playSuffix: String) =
    Seq(
      "ch.qos.logback"          %  "logback-core"               % "1.2.3",
      "com.kenshoo"             %% "metrics-play"               % "2.7.3_0.8.2", // this is compatible with play 2.8
      "com.typesafe.play"       %% "play-guice"                 % playVersion,
      "io.dropwizard.metrics"   %  "metrics-graphite"           % "4.1.17",
      "uk.gov.hmrc"             %% s"auth-client-$playSuffix"   % s"7.0.0-SNAPSHOT",
      "uk.gov.hmrc"             %% "crypto"                     % "7.4.0-SNAPSHOT",
      "uk.gov.hmrc"             %% s"http-verbs-$playSuffix"    % httpVerbsVersion,
      "uk.gov.hmrc"             %% s"play-auditing-$playSuffix" % "8.7.0-SNAPSHOT",
      // the following are not used by bootstrap - but transitively added for clients
      "com.typesafe.play"       %% (if (playVersion == play28Version) "filters-helpers" else "play-filters-helpers") % playVersion,
      "uk.gov.hmrc"             %% "logback-json-logger"        % "5.2.0",

      // test dependencies
      "com.typesafe.play"       %% "play-test"                  % playVersion    % Test,
      "org.mockito"             %% "mockito-scala-scalatest"    % "1.17.14"      % Test,
      "com.vladsch.flexmark"    %  "flexmark-all"               % "0.64.8"       % Test,
      "org.scalatestplus"       %% "scalacheck-1-17"            % "3.2.17.0"     % Test,
      "org.scalatestplus.play"  %% "scalatestplus-play"         % scalaTestPlusPlayVersion(playVersion) % Test,
    ) ++
      (
        if (playVersion == play28Version)
          // jackson overrides (CVE-2020-36518 mitigation)
          Seq(
            "com.fasterxml.jackson.core"       %  "jackson-core"                   % jacksonVersion,
            "com.fasterxml.jackson.core"       %  "jackson-annotations"            % jacksonVersion,
            "com.fasterxml.jackson.datatype"   %  "jackson-datatype-jdk8"          % jacksonVersion,
            "com.fasterxml.jackson.datatype"   %  "jackson-datatype-jsr310"        % jacksonVersion,
            "com.fasterxml.jackson.core"       %  "jackson-databind"               % jacksonDatabindVersion,
            "com.fasterxml.jackson.dataformat" %  "jackson-dataformat-cbor"        % jacksonVersion,
            "com.fasterxml.jackson.module"     %  "jackson-module-parameter-names" % jacksonVersion,
            "com.fasterxml.jackson.module"     %% "jackson-module-scala"           % jacksonVersion,

            "com.github.tomakehurst" %  "wiremock-jre8" % "2.27.2" % Test, // last version with jackson dependencies compatible with play
          )
        else Seq(
          "com.github.tomakehurst"  %  "wiremock"        % "3.0.0-beta-7" % Test
        )
      )

  private def frontendCommon(playVersion: String, playSuffix: String) =
    common(playVersion, playSuffix) :+ "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test

  private def test(playVersion: String, playSuffix: String) =
    Seq(
      "com.typesafe.play"       %% "play-test"                    % playVersion,
      "uk.gov.hmrc"             %% s"http-verbs-test-$playSuffix" % httpVerbsVersion,
      "org.scalatestplus.play"  %% "scalatestplus-play"           % scalaTestPlusPlayVersion(playVersion),
    ) ++
      // provides the optional dependency of scalatest as pulled in by scalatestplus-play
      (if (playVersion == play28Version)
        Seq("com.vladsch.flexmark"   %  "flexmark-all"       % "0.35.10")
       else
        Seq("com.vladsch.flexmark"   %  "flexmark-all"       % "0.62.2")// to go beyond requires Java 11 https://github.com/scalatest/scalatest/issues/2276
      ) ++
    Seq(
      // we use the same version of scalatest across play versions for simplicity for internal testing
      // but most clients probably just want to use the one provided transitively by scalatestplus-play
      "org.scalatest"           %% "scalatest"                    % "3.2.17"      % Test,
      "com.vladsch.flexmark"    %  "flexmark-all"                 % "0.64.8"      % Test,
      "com.typesafe.akka"       %% "akka-stream-testkit"          % akkaVersion   % Test,
      "com.typesafe.play"       %% "play-akka-http-server"        % playVersion   % Test
    )

  private def health(playVersion: String, playSuffix: String) =
    Seq(
      "com.typesafe.play"       %% "play"                       % playVersion,
      // test dependencies
      "org.scalatest"           %% "scalatest"                  % "3.2.17"      % Test,
      "com.vladsch.flexmark"    %  "flexmark-all"               % "0.64.8"      % Test,
      "org.scalatestplus.play"  %% "scalatestplus-play"         % scalaTestPlusPlayVersion(playVersion) % Test,
    )

  private def scalaTestPlusPlayVersion(playVersion: String): String =
    if (playVersion == play28Version) "5.1.0"
    else if (playVersion == play29Version) "6.0.0"
    else sys.error("Unsupported playVersion")
}
