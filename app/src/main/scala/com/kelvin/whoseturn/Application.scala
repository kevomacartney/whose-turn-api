package com.kelvin.whoseturn

import cats.effect._
import cats.implicits._
import com.codahale.metrics.MetricRegistry
import com.kelvin.whoseturn.admin.PrivateService
import com.kelvin.whoseturn.config.ApplicationConfig
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.{Router, Server}
import pureconfig.ConfigSource
import com.kelvin.whoseturn.config.ConfigOverrides._

import scala.io.{BufferedSource, Source}
import scala.jdk.CollectionConverters._

object Application extends LazyLogging {

  def run(env: String, configOverride: Config = ConfigFactory.empty()): IO[ExitCode] = {
    val runningAppResource = for {
      secrets         <- loadSecrets
      appConfig       <- loadConfig(secrets, configOverride)
      metricsRegistry <- Resource.eval(IO(new MetricRegistry))
      runningApp      <- Wiring.create(appConfig)(metricsRegistry)
      _               <- initOpsService(appConfig, metricsRegistry)
    } yield runningApp

    runningAppResource
      .use { server =>
        logger.info(s"Server running at address ${server.baseUri}")
        IO.never
      }
      .as(ExitCode.Success)
      .handleErrorWith { ex =>
        logger.error("Server terminated due to error", ex)
        IO.raiseError(ex)
      }
  }

  def loadConfig(secrets: Map[String, String], configOverride: Config): Resource[IO, ApplicationConfig] = {
    import pureconfig.generic.auto._ // required

    val defaultConfigResource = Resource.eval(IO(ConfigFactory.parseResources("application.conf")))
    val secretsConfigResource = Resource.eval(IO(ConfigFactory.parseMap(secrets.asJava)))

    for {
      defaultConfig <- defaultConfigResource
      secretsConfig <- secretsConfigResource
      mergedConfig = ConfigFactory
        .load()
        .withFallback(defaultConfig)
        .withFallback(secretsConfig)
        .withOverride(configOverride)
        .resolve()

      appConfig = ConfigSource.fromConfig(mergedConfig).loadOrThrow[ApplicationConfig]
    } yield appConfig
  }

  def loadSecrets: Resource[IO, Map[String, String]] = {
    import io.circe.parser._
    val acquire            = IO(Source.fromFile(this.getClass.getResource("/secrets.json").getPath))
    val release            = (bS: BufferedSource) => IO(bS.close())
    val fileBufferResource = Resource.make(acquire)(release)

    for {
      buffer <- fileBufferResource
      str    = buffer.getLines().seq.mkString("\n")
      json   = decode[Map[String, String]](str).getOrElse(throw new RuntimeException("Invalid secrets file"))
    } yield json

  }

  private def initOpsService(appConfig: ApplicationConfig, metricRegistry: MetricRegistry): Resource[IO, Server] = {
    val opsService = createOpsService(metricRegistry)
    BlazeServerBuilder[IO]
      .bindHttp(appConfig.opsServerConfig.port, "0.0.0.0")
      .withHttpApp(opsService.orNotFound)
      .resource
  }

  private def createOpsService(metricRegistry: MetricRegistry): HttpRoutes[IO] = {
    val httpService = new PrivateService(metricRegistry)
    val services    = httpService.alive() <+> httpService.metrics()
    Router(s"/private" -> services)
  }
}
