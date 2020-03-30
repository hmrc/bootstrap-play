import sbt._

object AppDependencies {

  private val playVersion     = "2.6.24"
  // 2.5.23 is last compatible version with reactivemongo
  // https://github.com/akka/akka/blob/master/akka-actor/src/main/mima-filters/2.5.23.backwards.excludes
  private val akkaVersion     = "2.5.23"
  private val akkaHttpVersion = "10.0.15"

  val compile: Seq[ModuleID] = Seq(
    "ch.qos.logback"        % "logback-core"         % "1.1.7",
    "com.kenshoo"           %% "metrics-play"        % "2.6.6_0.6.2",
    "com.typesafe.play"     %% "filters-helpers"     % playVersion,
    "com.typesafe.play"     %% "play"                % playVersion,
    "com.typesafe.play"     %% "play-guice"          % playVersion,
    "com.typesafe.play"     %% "play-ahc-ws"         % playVersion,
    "io.dropwizard.metrics" % "metrics-graphite"     % "3.2.5",
    "uk.gov.hmrc"           %% "auth-client"         % "2.33.0-play-26",
    "uk.gov.hmrc"           %% "crypto"              % "5.5.0",
    "uk.gov.hmrc"           %% "http-verbs"          % "10.6.0-play-26",
    "uk.gov.hmrc"           %% "logback-json-logger" % "4.6.0",
    "uk.gov.hmrc"           %% "play-auditing"       % "4.2.0-play-26",
    "uk.gov.hmrc"           %% "play-health"         % "3.14.0-play-26",
    "uk.gov.hmrc"           %% "time"                % "3.6.0",
    // force dependencies due to security flaws found in jackson-databind < 2.9.x using XRay
    "com.fasterxml.jackson.core"     % "jackson-core"            % "2.9.7",
    "com.fasterxml.jackson.core"     % "jackson-databind"        % "2.9.7",
    "com.fasterxml.jackson.core"     % "jackson-annotations"     % "2.9.7",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8"   % "2.9.7",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.9.7",
    // downgrade akka dependency for compatibility with reactivemongo (since play 2.6.23)
    "com.typesafe.akka" %% "akka-stream"    % akkaVersion     force(),
    "com.typesafe.akka" %% "akka-protobuf"  % akkaVersion     force(),
    "com.typesafe.akka" %% "akka-slf4j"     % akkaVersion     force(),
    "com.typesafe.akka" %% "akka-actor"     % akkaVersion     force(),
    "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion force()
  )

  val test: Seq[ModuleID] = Seq(
    "com.github.tomakehurst" % "wiremock-jre8"       % "2.21.0",
    "com.typesafe.play"      %% "play-test"          % playVersion,
    "org.mockito"            % "mockito-all"         % "1.9.5",
    "org.pegdown"            % "pegdown"             % "1.5.0",
    "org.scalacheck"         %% "scalacheck"         % "1.14.0",
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2"
  ).map(_ % Test)

}
