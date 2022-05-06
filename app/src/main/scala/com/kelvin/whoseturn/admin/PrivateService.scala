package com.kelvin.whoseturn.admin

import cats.effect.IO
import com.codahale.metrics.MetricRegistry
import org.http4s.HttpRoutes
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._

class PrivateService(metricRegistry: MetricRegistry) {
  def alive(): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case _ @GET -> Root / "alive" =>
      Ok()
  }
}
