import Dependencies._
import sbt.Keys.resolvers
import sbt._
import Resolvers._

lazy val `whose-turn-api` = (project in file("."))
  .settings(
    ThisBuild / scalaVersion := "2.13.5",
    ThisBuild / organization := "Kelvin Macartney",
    ThisBuild / version := "0.1",
  )
  .aggregate(`domain`, `app`, `end-to-end`, `test-support`, `web`)

lazy val commonSettings = Seq(
  resolvers := whoseTurnResolvers,
  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.0" cross CrossVersion.full),
  libraryDependencies ++= List(
    Cats.cats,
    Cats.catsEffect,
    Logging.scalaLogging,
    Logging.logBack,
    Logging.catsLogging,
    Time.nScalaTime,
    Testing.testFramework,
    Testing.testMockFramework
  ),
  dependencyOverrides ++= Dependencies.overrides,
  publish := {},
  //  allows for graceful shutdown of containers once the tests have finished running.
  Test / fork := true
)

lazy val `domain` = (project in file("./domain"))
  .dependsOn(`kafka`, `test-support`)
  .settings(commonSettings)
  .settings(
    name := "domain",
    libraryDependencies ++= List(
      Cassandra.dataStaxQueryBuilder,
      Circe.circeFs2,
      Fs2.fs2Core,
      Metrics.metricsCore,
      Kafka.`kafka-clients`
    )
  )

lazy val `app` = (project in file("./app"))
  .dependsOn(domain, `web`)
  .settings(commonSettings)
  .settings(Http4s.http4sAll)
  .settings(
    name := "app",
    libraryDependencies ++= List(
      Config.pureConfig,
      Fs2.fs2Core,
      Metrics.metricsCore,
      Metrics.metricsJson,
      Metrics.metricsJvm
    )
  )

lazy val `end-to-end` = (project in file("./end-to-end"))
  .dependsOn(`test-support`, `domain`, `app`)
  .settings(commonSettings)
  .settings(Http4s.http4sAll)
  .settings(
    name := "end-to-end",
    libraryDependencies ++= List(
      Config.pureConfig,
      Circe.circeOptics,
      Fs2.fs2Core,
      Metrics.metricsCore,
      Metrics.metricsJson,
      Metrics.metricsJvm
    )
  )

lazy val `kafka` = (project in file("./kafka"))
  .settings(commonSettings)
  .settings(
    name := "kafka",
    libraryDependencies ++= List(
      Kafka.`fs2-kafka`,
      Kafka.`fs2-kafka-vulcan`,
      Testing.simpleHttpClient % Test
    )
  )

lazy val `test-support` = (project in file("./test-support"))
  .settings(commonSettings)
  .settings(Http4s.http4sAll)
  .settings(
    name := "test-support",
    libraryDependencies ++= List(
      Metrics.metricsCore,
      Metrics.metricsJson,
      Metrics.metricsJvm,
      Kafka.`kafka-clients`,
      Kafka.`fs2-kafka`,
      Kafka.`kafka-avro`,
      Kafka.`kafka-avro`,
      Testing.`testContainer-kafka`,
      Testing.simpleHttpClient
    )
  )

lazy val `web` = (project in file("./web"))
  .dependsOn(`domain`)
  .settings(commonSettings)
  .settings(Http4s.http4sAll)
  .settings(
    name := "web",
    libraryDependencies ++= List(
      Fs2.fs2Core,
      Circe.circeCore,
      Circe.circeGeneric,
      Circe.circeParse,
      Circe.circeFs2,
      Metrics.metricsCore,
      Metrics.metricsJson,
      Metrics.metricsJvm,
      Cassandra.dataStax,
      Cassandra.dataStaxQueryBuilder,
      Doobie.`doobie-core`,
      Doobie.`doobie-postgresql`,
      Doobie.`doobie-scalatest`          % Test,
      Testing.testContainer              % Test,
      Testing.`testContainer-cassandra`  % Test,
      Testing.`testContainer-postgresql` % Test
    )
  )
