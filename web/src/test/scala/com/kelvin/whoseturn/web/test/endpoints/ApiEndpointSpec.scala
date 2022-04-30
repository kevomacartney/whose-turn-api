package com.kelvin.whoseturn.web.test.endpoints

import cats.effect.unsafe.IORuntime
import cats.effect._
import com.codahale.metrics.MetricRegistry
import com.kelvin.whoseturn.web.repositories.{ItemRepository, RepositoryItem}
import com.kelvin.whoseturn.web.services.ApiService
import io.circe.{Decoder, jawn}
import io.circe.generic.semiauto.deriveDecoder
import org.http4s._
import org.http4s.circe.jsonOf
import org.http4s.implicits._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ApiEndpointSpec extends AnyWordSpec with Matchers with ScalaFutures {
  import ApiEndpointSpec._

  implicit val runtime: IORuntime                                             = cats.effect.unsafe.IORuntime.global
  implicit val repositoryItemDecoder: Decoder[RepositoryItem]                 = deriveDecoder[RepositoryItem]
  implicit val repositoryItemEntityDecoder: EntityDecoder[IO, RepositoryItem] = jsonOf[IO, RepositoryItem]

  "GET/ get" should {
    "returns a com.kelvin.whoseturn.web.test.repository item" in {
      withService() { service =>
        val request  = buildSuccessRequest()
        val response = service(request).unsafeRunSync()

        response.status mustBe Status.Ok
        response.as[RepositoryItem].unsafeRunSync()
      }
    }

    "returns a failure with invalid input" in {
      withService() { service =>
        val request  = buildFailureRequest()
        val response = service(request).unsafeRunSync()

        response.status mustBe Status.BadRequest
      }
    }

    "Metrics get recorded" in {
      val metricRegistry = new MetricRegistry()

      withService(metricRegistry) { service =>
        val request = buildSuccessRequest()
        service(request).unsafeRunSync()

        metricRegistry.meter("retrievals").getCount mustBe 1
      }
    }
  }
}

object ApiEndpointSpec {
  import io.circe.generic.auto._

  type Service = Request[IO] => IO[Response[IO]]

  def withService[T](metricRegistry: MetricRegistry = new MetricRegistry())(f: Service => T): T = {
    val repository = new ItemRepository(metricRegistry)
    val service    = new ApiService(repository)(metricRegistry).helloWorldService.orNotFound.run
    f(service)
  }

  def buildSuccessRequest(): Request[IO] = {
    Request(method = Method.GET, uri = Uri.unsafeFromString("/get/success"))
  }

  def buildFailureRequest(): Request[IO] = {
    Request(method = Method.GET, uri = Uri.unsafeFromString("/get/failure"))
  }
}
