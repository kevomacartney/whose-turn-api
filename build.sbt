import Dependencies._
import sbt._

lazy val `whose-turn-api` = (project in file("."))
  .settings(
    ThisBuild / scalaVersion := "2.13.5",
    ThisBuild / organization := "Kelvin Macartney",
    ThisBuild / version := "0.1"
  )
  .aggregate(`domain`, `app`, `end-to-end`, `test-support`, `web`)

lazy val commonSettings = Seq(
  resolvers += "Confluent" at "https://packages.confluent.io/maven/",
  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.0" cross CrossVersion.full),
  libraryDependencies ++= List(
    Cats.cats,
    Cats.catsEffect,
    Logging.scalaLogging,
    Logging.logBack,
    Logging.catsLogging,
    Time.nScalaTime,
    Testing.testFramework,
    Testing.mockTestFramework
  ),
  dependencyOverrides ++= Dependencies.overrides,
  publish := {},
  //  allows for graceful shutdown of containers once the tests have finished running.
  Test / fork := true
)

lazy val `domain` = (project in file("./domain"))
  .settings(commonSettings)
  .settings(
    name := "domain",
    libraryDependencies ++= List(
      Cassandra.dataStaxQueryBuilder,
      Circe.circeFs2,
      Fs2.fs2Core,
      Metrics.metricsCore
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

lazy val `test-support` = (project in file("./test-support"))
  .dependsOn(`domain`)
  .settings(commonSettings)
  .settings(Http4s.http4sAll)
  .settings(
    name := "test-support",
    libraryDependencies ++= List(
      Metrics.metricsCore,
      Metrics.metricsJson,
      Metrics.metricsJvm
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
