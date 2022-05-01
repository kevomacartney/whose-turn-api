package com.kelvin.whoseturn.test.support

import cats.effect._
import cats.effect.unsafe.implicits.global
import com.kelvin.whoseturn.Application
import com.kelvin.whoseturn.test.support.e2e.TestContext
import io.circe._
import org.http4s._
import org.http4s.circe._
import org.scalatest.Assertion
import org.scalatest.concurrent._
import org.scalatest.matchers.must.Matchers

import scala.concurrent.duration.{DurationInt, FiniteDuration}

trait TestApplication extends ScalaFutures with IntegrationPatience with Matchers with Eventually {
  private val ServerPort    = 5024
  private val OpsServerPort = 5002

  def withTestApp[T]()(fn: TestContext => T): T = {
    implicit val context: TestContext = TestContext(serverPort = ServerPort)

    val testIo = for {
      runningServer <- Application.run("Acceptance").start
      _             <- IO(waitForAlive)
      result        <- IO(fn(context))
      _             <- runningServer.cancel
    } yield result

    testIo.unsafeRunSync()
  }

  protected def fetchMetrics()(implicit context: TestContext): Json = {
    context.executeRequestWithResponse(makeOpsServerRequest("/private/metrics")) { response =>
      response.as[Json].unsafeRunSync()
    }
  }

  private def waitForAlive(implicit context: TestContext): Assertion =
    eventually {
      context.executeRequestWithResponse(makeOpsServerRequest("/private/alive.txt")) { response =>
        response.status mustBe Status.Ok
      }
    }

  protected def makeOpsServerRequest(path: String, method: Method = Method.GET): Request[IO] = {
    val fullURl              = s"http://127.0.0.1:$OpsServerPort$path"
    val request: Request[IO] = Request(method, uri = Uri.unsafeFromString(fullURl))
    request
  }
}
