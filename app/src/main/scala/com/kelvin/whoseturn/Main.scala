package com.kelvin.whoseturn

import cats.effect._
import com.kelvin.whoseturn.config.ApplicationConfig
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import Application.logger
import pureconfig.ConfigSource

import scala.io.{BufferedSource, Source}
import scala.util.{Failure, Success, Try}

object Main extends IOApp with LazyLogging {
  override def run(args: List[String]): IO[ExitCode] = {
    for {
      env    <- getEnvironment
      exitCode <- Application.run(env)
    } yield exitCode
  }

  def getEnvironment: IO[String] = {
    System.getenv("ENVIRONMENT") match {
      case null => throw new Exception(s"Could not find required property: ENVIRONMENT.")
      case env  => IO(env)
    }
  }
}
