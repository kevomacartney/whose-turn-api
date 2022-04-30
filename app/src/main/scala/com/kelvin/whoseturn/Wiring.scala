package com.kelvin.whoseturn

import cats.effect._
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.jvm._
import com.kelvin.whoseturn.config.{ApplicationConfig, CassandraConfig}
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

import java.lang.management.ManagementFactory.getPlatformMBeanServer
import java.net.InetSocketAddress

object Wiring extends LazyLogging {
  def create(
      appConfig: ApplicationConfig
  ): Stream[IO, ExitCode] = {
    logger.info("Starting app")

    implicit val metricRegistry: MetricRegistry = initialiseMetrics()
    val service: HttpRoutes[IO]                 = createStateService(appConfig)

    BlazeServerBuilder[IO]
      .bindHttp(appConfig.restConfig.port, "0.0.0.0")
      .withHttpApp(service.orNotFound)
      .serve
  }

  private def initialiseMetrics(): MetricRegistry = {
    val metricsRegistry: MetricRegistry = new MetricRegistry
    registerJvmMetrics(metricsRegistry)

    metricsRegistry
  }

  private def registerJvmMetrics(metricsRegistry: MetricRegistry): Unit = {
    metricsRegistry.register("jvm.memory", new MemoryUsageGaugeSet)
    metricsRegistry.register("jvm.threads", new ThreadStatesGaugeSet)
    metricsRegistry.register("jvm.gc", new GarbageCollectorMetricSet)
    metricsRegistry.register("jvm.bufferpools", new BufferPoolMetricSet(getPlatformMBeanServer))
    metricsRegistry.register("jvm.classloading", new ClassLoadingGaugeSet)
    metricsRegistry.register("jvm.filedescriptor", new FileDescriptorRatioGauge)
  }

  private def createStateService(
      appConfig: ApplicationConfig
  )(implicit metricRegistry: MetricRegistry): HttpRoutes[IO] = {
    val httpService = new TodoStateService()
    val services    = httpService.add()
    val server      = Router(s"/api/${appConfig.restConfig.apiVersion}" -> services)

    Metrics[IO](Dropwizard(metricRegistry, "server"))(server)
  }

  private def createCassandraSession(config: CassandraConfig): CqlSession = {
    val contactPoint = InetSocketAddress.createUnresolved(config.contactPoint, config.port)
    val session      = CqlSession.builder().addContactPoint(contactPoint).build()

    logger.info(s"Created a Cassandra connection to [endpoint=${contactPoint.toString}]")
    session
  }
}
