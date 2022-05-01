package com.kelvin.whoseturn

import cats.effect._
import com.kelvin.whoseturn.config.ApplicationConfig
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import pureconfig.ConfigSource

import collection.JavaConverters._
import scala.io.{BufferedSource, Source}

object Application extends LazyLogging {

  def run(env: String): IO[ExitCode] = {
    val appResource = for {
      secrets   <- loadSecrets
      appConfig <- loadConfig(secrets)
      wiring    <- Wiring.create(appConfig)
    } yield wiring

    appResource
      .use { server =>
        IO {
          logger.info(s"Server running at address ${server.baseUri}")
          ExitCode.Success
        }
      }
      .handleErrorWith { ex =>
        logger.error("Server terminated due to error", ex)
        IO.raiseError(ex)
      }
  }

  def loadConfig(secrets: Map[String, String]): Resource[IO, ApplicationConfig] = { // required
    import pureconfig.generic.auto._

    val defaultConfigResource = Resource.eval(IO(ConfigFactory.parseResources("default.conf")))
    val secretsConfigResource = Resource.eval(IO(ConfigFactory.parseMap(secrets.asJava)))

    for {
      defaultConfig <- defaultConfigResource
      secretsConfig <- secretsConfigResource
      mergedConfig  = ConfigFactory.load().withFallback(defaultConfig).withFallback(secretsConfig).resolve()
      appConfig     = ConfigSource.fromConfig(mergedConfig).loadOrThrow[ApplicationConfig]
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
}
