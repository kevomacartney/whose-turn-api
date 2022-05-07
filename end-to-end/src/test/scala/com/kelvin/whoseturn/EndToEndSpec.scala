package com.kelvin.whoseturn

import cats.effect.IO
import com.kelvin.whoseturn.models.CreateTodoItemModel
import com.kelvin.whoseturn.support.TestApplication
import com.kelvin.whoseturn.support.fixtures.TodoItemsFixtures
import io.circe.generic.auto._
import org.http4s._
import org.http4s.circe._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.must.Matchers

class EndToEndSpec extends AnyFeatureSpec with TestApplication with ScalaFutures with Matchers with TodoItemsFixtures {
  Scenario("/api/v1/add returns HTTP 200 for valid body") {
    withTestApp() { context =>
      implicit val createModelEncoder: EntityEncoder[IO, CreateTodoItemModel] = jsonEncoderOf[IO, CreateTodoItemModel]
      val body = CreateTodoItemModelFixture()

      context.executeRequestWithResponse(url = "/api/v1/add", method = Method.PUT, body = body) { response =>
        response.status mustBe Status.Ok
      }
    }
  }

//  Scenario("Metrics are recorded") {
//    withTestApp() { implicit context =>
//      context.executeRequestWithResponse("/get/success")(_ => ())
//      context.executeRequestWithResponse("/get/failure")(_ => ())
//
//      val metrics = fetchMetrics()
//
//      root.meters.retrievals.count.long.getOption(metrics) mustBe Some(2)
//      root.meters.`item-repository.failure`.count.long.getOption(metrics) mustBe Some(1)
//      root.meters.`item-repository.success`.count.long.getOption(metrics) mustBe Some(1)
//    }
//  }
}
