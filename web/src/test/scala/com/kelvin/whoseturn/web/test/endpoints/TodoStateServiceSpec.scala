package com.kelvin.whoseturn.web.test.endpoints

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.codahale.metrics.MetricRegistry
import com.kelvin.whoseturn.entity.TodoItemEntity
import com.kelvin.whoseturn.errors.http._
import com.kelvin.whoseturn.errors.meta.BodyError
import com.kelvin.whoseturn.web.repositories._
import com.kelvin.whoseturn.web.services.TodoStateService
import com.kelvin.whoseturn.web.models.CreateTodoItemModel
import fs2.Stream
import io.circe._
import io.circe.generic.auto._
import org.http4s.circe._
import org.http4s._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import com.kelvin.whoseturn.web.test.testSupport.TodoItemsFixtures

class TodoStateServiceSpec extends AnyWordSpec with Matchers with ScalaFutures with TodoItemsFixtures {
  import TodoStateServiceSpec._

  "PUT /put" should {
    "successfully return HTTP 200 and write entity to repo" in {
      val context = TestContext(todoItemRepository = successfulMockedTodoItemRepo())

      withTodoService(context) { todoService =>
        val request  = buildSuccessfulAddRequest()
        val response = todoService(request).unsafeRunSync()

        response.status mustBe Status.Ok

        val responseEntity = response.as[TodoItemEntity].unsafeRunSync()
        compareTodoEntitiesOrFail(responseEntity)(TodoItemEntityFixture())
      }
    }

    "return HTTP 400 when body contains invalid fields" in {
      val context = TestContext(todoItemRepository = successfulMockedTodoItemRepo())
      withTodoService(context) { todoService =>
        val request  = buildFailingValidationAddRequest
        val response = todoService(request).unsafeRunSync()

        response.status mustBe Status.BadRequest

        val responseEntity = response.as[ValidationError].unsafeRunSync()
        responseEntity.errorLocation mustBe BodyError

        responseEntity.validatedFields must contain allOf (
          ValidatedField(field = "title", message = "title is a required field"),
          ValidatedField(field = "location", message = "location is a required field")
        )
      }
    }

    "return HTTP 400 when body is invalid Json" in {
      val context = TestContext(todoItemRepository = successfulMockedTodoItemRepo())
      withTodoService(context) { todoService =>
        val request  = buildEmptyBodyAddRequest
        val response = todoService(request).unsafeRunSync()

        response.status mustBe Status.BadRequest

        val responseEntity = response.as[GenericError].unsafeRunSync()
        responseEntity.message mustBe "There was a problem handling your request, it arrived malformed"
      }
    }

    "failed gracefully and return HTTP 500 when unexpected exception is thrown" in {
      val context = TestContext(todoItemRepository = failingMockedTodoItemRepo)

      withTodoService(context) { todoService =>
        val request  = buildSuccessfulAddRequest()
        val response = todoService(request).unsafeRunSync()

        response.status mustBe Status.InternalServerError

        val responseEntity = response.as[CriticalError].unsafeRunSync()
        responseEntity.message mustBe "There was an Internal Service Error, please try again."
      }
    }
  }
}

object TodoStateServiceSpec extends MockFactory with TodoItemsFixtures with Matchers {
  case class TestContext(todoItemRepository: TodoItemRepository[IO])
  type Service = Request[IO] => IO[Response[IO]]

  implicit val createModelEncoder: EntityEncoder[IO, CreateTodoItemModel] = jsonEncoderOf[IO, CreateTodoItemModel]
  implicit val todoItemDecoder: EntityDecoder[IO, TodoItemEntity]         = jsonOf[IO, TodoItemEntity]
  implicit val criticalErrorDecoder: EntityDecoder[IO, CriticalError]     = jsonOf[IO, CriticalError]
  implicit val genericErrorDecoder: EntityDecoder[IO, GenericError]       = jsonOf[IO, GenericError]
  implicit val validationErrorDecoder: EntityDecoder[IO, ValidationError] = jsonOf[IO, ValidationError]

  def withTodoService[T](
      context: TestContext
  )(f: Service => T)(implicit metricRegistry: MetricRegistry = new MetricRegistry()): T = {
    implicit val todoItemRepo: TodoItemRepository[IO] = context.todoItemRepository
    val service: Service                              = new TodoStateService().add().orNotFound.run

    f(service)
  }

  def successfulMockedTodoItemRepo(todoItemEntity: TodoItemEntity = TodoItemEntityFixture()): TodoItemRepository[IO] = {
    val mockedRepo: TodoItemRepository[IO] = mock[TodoItemRepository[IO]]
    (mockedRepo.addItem _)
      .expects(*)
      .onCall { param: TodoItemEntity =>
        compareTodoEntitiesOrFail(param)(todoItemEntity)
        IO(param)
      }
      .once()

    mockedRepo
  }

  def failingMockedTodoItemRepo: TodoItemRepository[IO] = {
    val mockedRepo: TodoItemRepository[IO] = mock[TodoItemRepository[IO]]

    (mockedRepo.addItem _)
      .expects(*)
      .returning(IO.raiseError(new Exception("BOOOOM")))

    mockedRepo
  }

  def buildSuccessfulAddRequest(createModel: CreateTodoItemModel = CreateTodoItemModelFixture()): Request[IO] = {
    val body = createModelEncoder.toEntity(createModel).body
    Request(method = Method.PUT, uri = Uri.unsafeFromString("/add"), body = body)
  }

  def buildFailingValidationAddRequest: Request[IO] = {
    val createModel = CreateTodoItemModelFixture(title = "", location = "")
    val body        = createModelEncoder.toEntity(createModel).body

    Request(method = Method.PUT, uri = Uri.unsafeFromString("/add"), body = body)
  }

  def buildEmptyBodyAddRequest: Request[IO] = {
    Request(method = Method.PUT, uri = Uri.unsafeFromString("/add"))
  }

  def compareTodoEntitiesOrFail(param: TodoItemEntity)(todoItemEntity: TodoItemEntity): Unit = {
    param.title mustBe todoItemEntity.title
    param.description mustBe todoItemEntity.description
    param.flagged mustBe todoItemEntity.flagged
    param.category mustBe todoItemEntity.category
    param.priority mustBe todoItemEntity.priority
    param.location mustBe todoItemEntity.location
    param.active mustBe todoItemEntity.active
  }
}
