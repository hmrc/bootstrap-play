import sbt._

object LibDependencies {

  private val play26Version  = "2.6.25"
  private val play27Version  = "2.7.4"

  val commonPlay26: Seq[ModuleID] = common(play26Version, "play-26")
  val frontendCommonPlay26: Seq[ModuleID] = frontendCommon(play26Version, "play-26")
  val commonPlay27: Seq[ModuleID] = common(play27Version, "play-27")
  val frontendCommonPlay27: Seq[ModuleID] = frontendCommon(play27Version, "play-27")

  val testPlay26: Seq[ModuleID] = test(play26Version)
  val testPlay27: Seq[ModuleID] = test(play27Version)

  val healthPlay26: Seq[ModuleID] = health(play26Version, "play-26")
  val healthPlay27: Seq[ModuleID] = health(play27Version, "play-27")

  private def common(playVersion: String, playSuffix: String) =
    Seq(
      "ch.qos.logback"         %  "logback-core"               % "1.2.3",
      "com.kenshoo"            %% "metrics-play"               % (if (playVersion == play26Version) "2.6.19_0.7.0"
                                                                  else "2.7.3_0.8.2" // scala_2_12 only
                                                                 ),
      "com.typesafe.play"      %% "play-guice"                 % playVersion,
      "io.dropwizard.metrics"  %  "metrics-graphite"           % "4.1.5",
      "uk.gov.hmrc"            %% "auth-client"                % s"3.0.0-$playSuffix",
      "uk.gov.hmrc"            %% "crypto"                     % "5.6.0",
      "uk.gov.hmrc"            %% s"http-verbs-$playSuffix"    % "12.0.0",
      "uk.gov.hmrc"            %% s"play-auditing-$playSuffix" % "6.0.0",
      // the following are not used by bootstrap - but transitively added for clients
      "com.typesafe.play"      %% "filters-helpers"            % playVersion,
      "uk.gov.hmrc"            %% "logback-json-logger"        % "4.8.0",
      // test dependencies
      "com.github.tomakehurst" %  "wiremock-jre8"              % "2.26.3"                % Test,
      "com.typesafe.play"      %% "play-test"                  % playVersion             % Test,
      "org.mockito"            %% "mockito-scala"              % "1.14.8"                % Test,
      "org.mockito"            %% "mockito-scala-scalatest"    % "1.14.8"                % Test,
      "com.vladsch.flexmark"   %  "flexmark-all"               % "0.35.10"               % Test,
      "org.scalacheck"         %% "scalacheck"                 % "1.14.3"                % Test,
      "org.scalatestplus"      %% "scalatestplus-mockito"      % "1.0.0-M2"              % Test,
      "org.scalatestplus.play" %% "scalatestplus-play"         % (if (playVersion == play26Version) "3.1.3"
                                                                  else "4.0.3"
                                                                 )                       % Test,
      "org.scalatestplus"      %% "scalatestplus-scalacheck"   % "3.1.0.0-RC2"           % Test
    )

  private def frontendCommon(playVersion: String, playSuffix: String) = common(playVersion, playSuffix) ++
    Seq(
      "uk.gov.hmrc"            %% "play-whitelist-filter"      % s"3.4.0-$playSuffix"
    )

  private def test(playVersion: String) =
    Seq(
      "com.typesafe.play"      %% "play-test"                  % playVersion,
      // test dependencies
      "org.scalatest"          %% "scalatest"                  % "3.1.1"                 % Test,
      "com.vladsch.flexmark"   %  "flexmark-all"               % "0.35.10"               % Test
    )

  private def health(playVersion: String, playSuffix: String) =
    Seq(
      "com.typesafe.play"      %% "play"                       % playVersion,
      // test dependencies
      "org.scalatest"          %% "scalatest"                  % "3.1.1"                 % Test,
      "com.vladsch.flexmark"   %  "flexmark-all"               % "0.35.10"               % Test,
      "org.scalatestplus.play" %% "scalatestplus-play"         % (if (playVersion == play26Version) "3.1.3"
                                                                  else "4.0.3"
                                                                 )                       % Test
    )
}
