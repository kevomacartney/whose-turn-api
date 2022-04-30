package com.kelvin.whoseturn.web.repositories

import cats.effect.{IO, Resource}
import com.codahale.metrics.MetricRegistry

import java.util.UUID

class ItemRepository(metricRegistry: MetricRegistry) extends Repository[IO] {
  override def get(id: String): IO[Option[RepositoryItem]] = {
    id match {
      case "failure" => incrementFailureMetrics() >> IO(None)
      case _         => incrementSuccessMetrics() >> retrieveItem()
    }
  }

  private def retrieveItem(): IO[Option[RepositoryItem]] =
    IO(Some(RepositoryItem(name = "com.kelvin.whoseturn.web.test.repository-item", id = UUID.randomUUID())))

  private def incrementFailureMetrics(): IO[Unit] =
    IO(metricRegistry.meter("item-com.kelvin.whoseturn.web.test.repository.failure").mark())

  private def incrementSuccessMetrics(): IO[Unit] =
    IO(metricRegistry.meter("item-com.kelvin.whoseturn.web.test.repository.success").mark())
}

object ItemRepository {
  def apply()(implicit metricRegistry: MetricRegistry): Resource[IO, ItemRepository] = {
    val acquire = IO(new ItemRepository(metricRegistry))
    Resource.eval(acquire)
  }
}