package com.kelvin.whoseturn

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.kelvin.whoseturn.models.CreateTodoItemModel
import com.kelvin.whoseturn.support.TestApplication
import com.kelvin.whoseturn.support.fixtures.TodoItemsFixtures
import io.circe._
import io.circe.optics.JsonPath.root
import io.circe.generic.auto._
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.must.Matchers

class EndToEndSpec extends AnyFeatureSpec with TestApplication with ScalaFutures with Matchers with TodoItemsFixtures {
  Scenario("/api/v1/add returns HTTP 200 for valid body") {
    withTestApp() { context =>
      implicit val createModelEncoder: EntityEncoder[IO, CreateTodoItemModel] = jsonEncoderOf[IO, CreateTodoItemModel]
      val body                                                                = CreateTodoItemModelFixture()

      context.executeRequestWithResponse(url = "/api/v1/add", method = Method.PUT, body = body) { response =>
        response.status mustBe Status.Ok
      }
    }
  }

  Scenario("/api/v1/add Metrics are recorded") {
    withTestApp() { context =>
      implicit val createModelEncoder: EntityEncoder[IO, CreateTodoItemModel] = jsonEncoderOf[IO, CreateTodoItemModel]
      val body                                                                = CreateTodoItemModelFixture()
      context.executeRequestWithResponse(url = "/api/v1/add", method = Method.PUT, body = body)(_ => ())

      context.executeOpsRequestWithResponse(url = "/metrics", method = Method.GET) { response =>
        response.status mustBe Status.Ok

        val json = response.as[Json].unsafeRunSync()

        root.meters.`service.todostate.create`.count.int.getOption(json).get mustBe 1
      }
    }
  }
}
