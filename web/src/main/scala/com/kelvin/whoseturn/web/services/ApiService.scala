package com.kelvin.whoseturn.web.services

import cats.effect._
import com.codahale.metrics.MetricRegistry
import com.kelvin.whoseturn.web.repositories.{ItemRepository, RepositoryItem}
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe.jsonEncoder
import org.http4s.dsl.io._

class ApiService(repository: ItemRepository)(implicit metricRegistry: MetricRegistry) {
  implicit val itemRepositoryEncoder: Encoder[RepositoryItem] = deriveEncoder[RepositoryItem]

  def helloWorldService: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "get" / passCriteria =>
      metricRegistry.meter("retrievals").mark()

      repository.get(passCriteria).flatMap {
        case None           => BadRequest()
        case Some(repoItem) => Ok(repoItem.asJson)
      }
  }

}
