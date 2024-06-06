import sbt._

object LibDependencies {
  private val httpVerbsVersion  = "15.0.0"
  private val akkaVersion       = "2.6.21"
  private val pekkoVersion      = "1.0.2"
  private val dropwizardVersion = "4.2.22"

  def common(playSuffix: String) =
    Seq(
      "ch.qos.logback"          %  "logback-core"               % "1.2.3",
      playOrg(playSuffix)       %% "play-guice"                 % playVersion(playSuffix),
      "io.dropwizard.metrics"   %  "metrics-graphite"           % dropwizardVersion,
      "io.dropwizard.metrics"   %  "metrics-jvm"                % dropwizardVersion,
      "io.dropwizard.metrics"   %  "metrics-logback"            % dropwizardVersion,
      "uk.gov.hmrc"             %% s"auth-client-$playSuffix"   % "8.0.0",
      "uk.gov.hmrc"             %% "crypto"                     % "8.0.0",
      "uk.gov.hmrc"             %% s"http-verbs-$playSuffix"    % httpVerbsVersion,
      "uk.gov.hmrc"             %% s"play-auditing-$playSuffix" % "9.0.0",
      // the following are not used by bootstrap - but transitively added for clients
      playOrg(playSuffix)       %% (if (playSuffix == "play-28") "filters-helpers"
                                    else                         "play-filters-helpers"
                                   )                            % playVersion(playSuffix),
      "uk.gov.hmrc"             %% "logback-json-logger"        % "5.4.0",

      // test dependencies
      playOrg(playSuffix)       %% "play-test"                  % playVersion(playSuffix)    % Test,
      "org.scalatestplus"       %% "mockito-3-4"                % "3.2.10.0"     % Test,
      "com.vladsch.flexmark"    %  "flexmark-all"               % "0.64.8"       % Test,
      "org.scalatestplus"       %% "scalacheck-1-17"            % "3.2.17.0"     % Test,
      "org.scalatestplus.play"  %% "scalatestplus-play"         % scalaTestPlusPlayVersion(playSuffix) % Test,
    ) ++
      (
        if (playSuffix == "play-28") {
          // jackson overrides (CVE-2020-36518 mitigation)
          val jacksonVersion         = "2.12.7"
          val jacksonDatabindVersion = "2.12.7.1"
          Seq(
            "com.fasterxml.jackson.core"       %  "jackson-core"                   % jacksonVersion,
            "com.fasterxml.jackson.core"       %  "jackson-annotations"            % jacksonVersion,
            "com.fasterxml.jackson.datatype"   %  "jackson-datatype-jdk8"          % jacksonVersion,
            "com.fasterxml.jackson.datatype"   %  "jackson-datatype-jsr310"        % jacksonVersion,
            "com.fasterxml.jackson.core"       %  "jackson-databind"               % jacksonDatabindVersion,
            "com.fasterxml.jackson.dataformat" %  "jackson-dataformat-cbor"        % jacksonVersion,
            "com.fasterxml.jackson.module"     %  "jackson-module-parameter-names" % jacksonVersion,
            "com.fasterxml.jackson.module"     %% "jackson-module-scala"           % jacksonVersion,

            "com.github.tomakehurst" % "wiremock-jre8" % "2.27.2"       % Test, // last version with jackson dependencies compatible with play
          )
        } else
          Seq(
            "com.github.tomakehurst" % "wiremock"      % "3.0.0-beta-7" % Test  // last version with jackson dependencies compatible with play
          )
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
      playOrg(playSuffix)       %% "play-test"                    % playVersion(playSuffix),
      "uk.gov.hmrc"             %% s"http-verbs-test-$playSuffix" % httpVerbsVersion,
      "org.scalatestplus.play"  %% "scalatestplus-play"           % scalaTestPlusPlayVersion(playSuffix),
      // provides the optional dependency of scalatest as pulled in by scalatestplus-play
      "com.vladsch.flexmark"    %  "flexmark-all"                 % (if (playSuffix == "play-28") "0.35.10"
                                                                     else                         "0.64.8"
                                                                    ),
      // we use the same version of scalatest across play versions for simplicity for internal testing
      // but most clients probably just want to use the one provided transitively by scalatestplus-play
      "org.scalatest"           %% "scalatest"                    % "3.2.18"      % Test,
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
      "org.scalatest"           %% "scalatest"                  % "3.2.18"      % Test,
      "com.vladsch.flexmark"    %  "flexmark-all"               % "0.64.8"      % Test,
      "org.scalatestplus.play"  %% "scalatestplus-play"         % scalaTestPlusPlayVersion(playSuffix) % Test,
    )

  private def playVersion(playSuffix: String) =
    playSuffix match {
      case "play-28" => "2.8.22"
      case "play-29" => "2.9.3"
      case "play-30" => "3.0.3"
    }

  private def playOrg(playSuffix: String): String =
    playSuffix match {
      case "play-28" => "com.typesafe.play"
      case "play-29" => "com.typesafe.play"
      case "play-30" => "org.playframework"
    }

  private def scalaTestPlusPlayVersion(playSuffix: String): String =
    playSuffix match {
      case "play-28" => "5.1.0"
      case "play-29" => "6.0.1"
      case "play-30" => "7.0.1"
    }
}
