package com.kelvin.whoseturn

import cats.effect._
import cats.implicits._
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.jvm._
import com.kelvin.whoseturn.config._
import com.kelvin.whoseturn.web.services.TodoStateService
import com.typesafe.scalalogging.LazyLogging
import fs2.Stream
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.implicits._
import org.http4s.metrics.dropwizard.Dropwizard
import org.http4s.server._
import org.http4s.server.middleware.Metrics
import com.datastax.oss.driver.api.core.CqlSession
import com.kelvin.whoseturn.admin.PrivateService
import com.kelvin.whoseturn.web.repositories._

import java.lang.management.ManagementFactory.getPlatformMBeanServer
import java.net.InetSocketAddress

object Wiring extends LazyLogging {
  def create(
      appConfig: ApplicationConfig
  )(implicit metricRegistry: MetricRegistry): Resource[IO, Server] = {
    logger.info("Starting app")

    val todoItemRepositoryResource = PostgresqlTodoItemRepository.resource(appConfig.postgresqlConfig)

    todoItemRepositoryResource.flatMap { todoItemRepository =>
      val businessServices: HttpRoutes[IO] = createServices(appConfig, todoItemRepository)

      BlazeServerBuilder[IO]
        .bindHttp(appConfig.restConfig.port, "0.0.0.0")
        .withHttpApp(businessServices.orNotFound)
        .resource
    }
  }

  private def createServices(
      appConfig: ApplicationConfig,
      todoItemRepository: TodoItemRepository[IO]
  )(implicit metricRegistry: MetricRegistry): HttpRoutes[IO] = {
    val httpService = new TodoStateService(todoItemRepository)
    val services    = httpService.add()
    val server      = Router(s"/api/${appConfig.restConfig.apiVersion}" -> services)

    Metrics[IO](Dropwizard(metricRegistry, "http.api."))(server)
  }
}
