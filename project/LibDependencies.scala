import sbt._

object LibDependencies {
  private val httpVerbsVersion  = "15.2.0"
  private val akkaVersion       = "2.6.21"
  private val pekkoVersion      = "1.0.3"
  private val dropwizardVersion = "4.2.32"

  def common(playSuffix: String) =
    Seq(
      playOrg(playSuffix)       %% "play-guice"                 % playVersion(playSuffix),
      "io.dropwizard.metrics"   %  "metrics-graphite"           % dropwizardVersion,
      "io.dropwizard.metrics"   %  "metrics-jvm"                % dropwizardVersion,
      "io.dropwizard.metrics"   %  "metrics-logback"            % dropwizardVersion,
      "uk.gov.hmrc"             %% s"auth-client-$playSuffix"   % "8.6.0",
      "uk.gov.hmrc"             %% "crypto"                     % "8.2.0",
      "uk.gov.hmrc"             %% s"http-verbs-$playSuffix"    % httpVerbsVersion,
      "uk.gov.hmrc"             %% s"play-auditing-$playSuffix" % "9.4.0",
      // the following are not used by bootstrap - but transitively added for clients
      playOrg(playSuffix)       %% "play-filters-helpers"       % playVersion(playSuffix),
      "uk.gov.hmrc"             %% "logback-json-logger"        % "5.5.0",

      // test dependencies
      "org.scalatestplus.play"  %% "scalatestplus-play"         % scalaTestPlusPlayVersion(playSuffix) % Test,
      "org.scalatest"           %% "scalatest"                  % "3.2.17"       % Test,
      "org.scalatestplus"       %% "mockito-4-11"               % "3.2.17.0"     % Test, // added explicitly, since not provided for Play 2.8 by scalatestplus-play
      "com.vladsch.flexmark"    %  "flexmark-all"               % "0.64.8"       % Test,
      "org.scalatestplus"       %% "scalacheck-1-17"            % "3.2.17.0"     % Test,
      "com.github.tomakehurst"  %  "wiremock"                   % "3.0.0-beta-7" % Test  // last version with jackson dependencies compatible with play
    )

  def backend(playSuffix: String) =
    common(playSuffix)

  def frontend(playSuffix: String) =
    common(playSuffix) :+
      (if (playSuffix == "play-30")
         "org.apache.pekko"     %% "pekko-stream-testkit"         % pekkoVersion  % Test
       else
         "com.typesafe.akka"    %% "akka-stream-testkit"          % akkaVersion   % Test
      )

  def test(playSuffix: String) =
    Seq(
      "uk.gov.hmrc"             %% s"http-verbs-test-$playSuffix" % httpVerbsVersion,
      "org.scalatestplus.play"  %% "scalatestplus-play"           % scalaTestPlusPlayVersion(playSuffix),
      // this is already provided by scalatestplus-play, but we want the latest version
      playOrg(playSuffix)       %% "play-test"                    % playVersion(playSuffix),
      // provides the optional dependency of scalatest as pulled in by scalatestplus-play
      "com.vladsch.flexmark"    %  "flexmark-all"                 % "0.64.8",
      // we use the same version of scalatest across play versions for simplicity for internal testing
      // but most clients probably just want to use the one provided transitively by scalatestplus-play
      "org.scalatest"           %% "scalatest"                    % "3.2.17"      % Test,
      "com.vladsch.flexmark"    %  "flexmark-all"                 % "0.64.8"      % Test,
      (if (playSuffix == "play-30")
         "org.apache.pekko"     %% "pekko-stream-testkit"         % pekkoVersion  % Test
       else
         "com.typesafe.akka"    %% "akka-stream-testkit"          % akkaVersion   % Test
      ),
      playOrg(playSuffix)      %% (if (playSuffix == "play-30") "play-pekko-http-server"
                                   else                         "play-akka-http-server"
                                  )                               % playVersion(playSuffix) % Test
    )

  def health(playSuffix: String) =
    Seq(
      playOrg(playSuffix)       %% "play"                       % playVersion(playSuffix),
      // test dependencies
      "org.scalatestplus.play"  %% "scalatestplus-play"         % scalaTestPlusPlayVersion(playSuffix) % Test,
      "org.scalatest"           %% "scalatest"                  % "3.2.17"      % Test,
      "com.vladsch.flexmark"    %  "flexmark-all"               % "0.64.8"      % Test
    )

  private def playVersion(playSuffix: String) =
    playSuffix match {
      case "play-29" => "2.9.7"
      case "play-30" => "3.0.7"
    }

  private def playOrg(playSuffix: String): String =
    playSuffix match {
      case "play-29" => "com.typesafe.play"
      case "play-30" => "org.playframework"
    }

  private def scalaTestPlusPlayVersion(playSuffix: String): String =
    playSuffix match {
      case "play-29" => "6.0.1"
      case "play-30" => "7.0.1"
    }
}
