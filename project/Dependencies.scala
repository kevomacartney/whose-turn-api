import sbt.Keys.libraryDependencies
import sbt._

object Dependencies {

  val overrides = List(
    "io.circe" %% "circe-parser" % "0.14.1"
  )

  object Circe {
    private val version = "0.14.1"

    lazy val circeCore    = "io.circe" %% "circe-core"    % version
    lazy val circeGeneric = "io.circe" %% "circe-generic" % version
    lazy val circeParse   = "io.circe" %% "circe-parser"  % version
    lazy val circeOptics  = "io.circe" %% "circe-optics"  % version
    lazy val circeFs2     = "io.circe" %% "circe-fs2"     % "0.14.0"
  }

  object Cats {
    lazy val cats       = "org.typelevel"    %% "cats-core"   % "2.6.1"
    lazy val catsEffect = "org.typelevel"    %% "cats-effect" % "3.2.7"
    lazy val catsRetry  = "com.github.cb372" %% "cats-retry"  % "3.1.0"
  }

  object Logging {
    lazy val logBack      = "ch.qos.logback"             % "logback-classic" % "1.2.5"
    lazy val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging"  % "3.9.4"
    lazy val jclOverSL4J  = "org.slf4j"                  % "jcl-over-slf4j"  % "1.7.32"
    lazy val catsLogging  = "org.typelevel"              %% "log4cats-slf4j" % "2.2.0"

  }

  object Time {
    lazy val nScalaTime = "com.github.nscala-time" %% "nscala-time" % "2.28.0"
  }

  object Config {
    lazy val pureConfig = "com.github.pureconfig" %% "pureconfig" % "0.16.0"
  }

  object Http4s {
    private lazy val http4sVersion = "0.23.9"

    lazy val http4sDsl        = "org.http4s" %% "http4s-dsl"                % http4sVersion
    lazy val http4sServer     = "org.http4s" %% "http4s-blaze-server"       % http4sVersion
    lazy val http4sClient     = "org.http4s" %% "http4s-blaze-client"       % http4sVersion
    lazy val http4sCirce      = "org.http4s" %% "http4s-circe"              % http4sVersion
    lazy val http4sDropwizard = "org.http4s" %% "http4s-dropwizard-metrics" % http4sVersion

    val http4sAll = Seq(
      libraryDependencies ++= List(
        Http4s.http4sServer,
        Http4s.http4sClient,
        Http4s.http4sDsl,
        Http4s.http4sCirce,
        http4sDropwizard
      )
    )
  }

  object Fs2 {
    private lazy val version = "3.2.4"
    lazy val fs2Core         = "co.fs2" %% "fs2-core" % version
  }

  object Testing {
    lazy val testFramework = "org.scalatest" %% "scalatest" % "3.2.9" % "test"

    lazy val testContainer              = "com.dimafeng"       %% "testcontainers-scala-scalatest"  % "0.39.12"
    lazy val `testContainer-cassandra`  = "org.testcontainers" % "cassandra"                        % "1.16.3"
    lazy val `testContainer-postgresql` = "com.dimafeng"       %% "testcontainers-scala-postgresql" % "0.40.0"
  }

  object Netty {
    private lazy val version = "4.1.63.Final"
    val all                  = "io.netty" % "netty-all" % version
  }

  object Cassandra {
    private lazy val version = "4.13.0"

    val dataStaxQueryBuilder = "com.datastax.oss" % "java-driver-query-builder" % version
    val dataStax             = "com.datastax.oss" % "java-driver-core"          % version
  }

  object Doobie {
    private lazy val version = "1.0.0-M5"

    lazy val `doobie-core`       = "org.tpolecat" %% "doobie-core"      % version
    lazy val `doobie-postgresql` = "org.tpolecat" %% "doobie-postgres"  % version
    lazy val `doobie-scalatest`  = "org.tpolecat" %% "doobie-scalatest" % version
  }

  object Metrics {
    private lazy val dropWizardMetricsVersion = "4.2.3"

    val metricsCore = "io.dropwizard.metrics" % "metrics-core" % dropWizardMetricsVersion
    val metricsJson = "io.dropwizard.metrics" % "metrics-json" % dropWizardMetricsVersion
    val metricsJvm  = "io.dropwizard.metrics" % "metrics-jvm"  % dropWizardMetricsVersion
  }
}
