package com.kelvin.whoseturn.e2e

import cats.effect.IO
import com.kelvin.whoseturn.models.CreateTodoItemModel
import com.kelvin.whoseturn.support.TestApplication
import io.circe.generic.auto.exportDecoder
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.featurespec.AnyFeatureSpec
import io.circe.optics.JsonPath.root
import org.scalatest.matchers.must.Matchers
import org.http4s._
import org.http4s.circe._
import org.http4s.headers.`Content-Type`
import testSupport.TodoItemsFixtures
import io.circe.Json
import io.circe.generic.auto._

class EndToEndSpec extends AnyFeatureSpec with TestApplication with ScalaFutures with Matchers with TodoItemsFixtures {
  Scenario("/api/v1/add Metrics are record") {
    withTestApp() { context =>
      val createModelEncoder: EntityEncoder[IO, CreateTodoItemModel] = jsonEncoderOf[IO, CreateTodoItemModel]
      val entity                                                     = createModelEncoder.toEntity(CreateTodoItemModelFixture())

      context.executeRequestWithResponse(url = "/api/v1/add", method = Method.PUT, body = entity.body) { response =>
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
