package com.kelvin.whoseturn.admin

import cats.effect.IO
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.json.MetricsModule
import com.fasterxml.jackson.databind.ObjectMapper
import com.typesafe.scalalogging.LazyLogging
import org.http4s.HttpRoutes
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import io.circe.parser._

import java.util.concurrent.TimeUnit

class PrivateService(metricRegistry: MetricRegistry) extends LazyLogging {
  private val jsonMapperForMetrics: ObjectMapper = {
    val mapper = new ObjectMapper
    mapper.registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.MILLISECONDS, false))
  }

  def alive(): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case _ @GET -> Root / "alive" =>
      Ok()
  }

  def metrics(): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case _ @GET -> Root / "metrics" =>
      val metricsRawJson = jsonMapperForMetrics.writeValueAsString(metricRegistry)

      parse(metricsRawJson).fold(
        error => {
          logger.error("Could not parse metrics as json", error)
          InternalServerError()
        },
        json => Ok(json)
      )
  }
}
