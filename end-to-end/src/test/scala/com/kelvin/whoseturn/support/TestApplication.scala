package com.kelvin.whoseturn.support

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.kelvin.whoseturn.Application
import com.kelvin.whoseturn.support.test.e2e.TestContext
import com.kelvin.whoseturn.support.test.postgresql.{PostgresqlContext, PostgresqlTestSupport}
import com.typesafe.config.{Config, ConfigFactory}
import io.circe.Json
import org.http4s.circe.jsonDecoder
import org.http4s.{Method, Request, Status, Uri}
import org.scalatest.Assertion
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers

import java.net.ServerSocket
import scala.jdk.CollectionConverters._

trait TestApplication
    extends ScalaFutures
    with IntegrationPatience
    with Matchers
    with Eventually
    with PostgresqlTestSupport {
  private val ServerPort    = getFreePort
  private val OpsServerPort = getFreePort

  def withTestApp[T]()(fn: TestContext => T): T = {
    withPostgresql { _ =>
      implicit val context: TestContext = TestContext(serverPort = ServerPort)

      val testIo = for {
        runningServer <- Application.run("Acceptance", createE2eConfig).start
        _             <- IO(waitForAlive)
        result        <- IO(fn(context))
        _             <- runningServer.cancel
      } yield result

      testIo.unsafeRunSync()
    }
  }

  protected def fetchMetrics()(implicit context: TestContext): Json = {
    context.executeRequestWithResponse(makeOpsServerRequest("/private/metrics")) { response =>
      response.as[Json].unsafeRunSync()
    }
  }

  private def waitForAlive(implicit context: TestContext): Assertion =
    eventually {
      context.executeRequestWithResponse(makeOpsServerRequest("/private/alive")) { response =>
        response.status mustBe Status.Ok
      }
    }

  protected def makeOpsServerRequest(path: String, method: Method = Method.GET): Request[IO] = {
    val fullURl              = s"http://127.0.0.1:$OpsServerPort$path"
    val request: Request[IO] = Request(method, uri = Uri.unsafeFromString(fullURl))
    request
  }

  protected def createE2eConfig: Config = {
    val configMap = Map(
      "postgresql-config.port" -> PostgreSqlTestContainer.getMappedPort(5432),
      "rest-config.port"       -> ServerPort,
      "ops-server-config.port" -> OpsServerPort
    ).asJava

    ConfigFactory.parseMap(configMap)
  }

  protected def getFreePort: Int = {
    val socket = new ServerSocket(0)
    val port   = socket.getLocalPort
    socket.close()

    port
  }

  implicit val context: PostgresqlContext = {
    val scriptPath   = getClass.getResource("/create_todo_items.sql").getPath
    val jsonDataPath = getClass.getResource("/todo_items_dump.json").getPath

    PostgresqlContext(scriptPath, jsonDataPath)
  }
}
