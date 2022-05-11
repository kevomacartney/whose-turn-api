package com.kelvin.whoseturn.web.services

import cats.syntax._
import cats.implicits._
import cats.data._
import com.codahale.metrics.MetricRegistry
import com.kelvin.whoseturn.web.repositories.TodoItemRepository
import cats.effect.IO
import com.kelvin.whoseturn.entities.TodoItemEntity
import com.kelvin.whoseturn.errors.Error
import com.kelvin.whoseturn.errors.meta.BodyError
import com.kelvin.whoseturn.errors.http._
import com.kelvin.whoseturn.exceptions.UnexpectedErrorException
import com.kelvin.whoseturn.todo.Priority._
import com.kelvin.whoseturn.implicits.TimestampImplicits._
import com.kelvin.whoseturn.models.CreateTodoItemModel
import com.kelvin.whoseturn.web.metrics.TodoStateServiceMetrics
import com.kelvin.whoseturn.web.services.TodoStateServiceValidation._
import com.typesafe.scalalogging.LazyLogging
import fs2.Stream
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._

class TodoStateService(todoItemRepository: TodoItemRepository[IO])(implicit metricRegistry: MetricRegistry)
    extends ServiceHelpers
    with LazyLogging
    with TodoStateServiceMetrics {

  import TodoStateService._

  def add(): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ PUT -> Root / "add" =>
      req
        .as[CreateTodoItemModel]
        .map(validateCreateTodoItemModel)
        .flatMap(addTodoEntityToRepo)
        .flatMap(createValidationResponse[TodoItemEntity])
        .map { resp =>
          incrementTodoItemCreatedCounter()
          resp
        }
        .handleErrorWith(handleIOError)

  }

  def get(): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "get" / UUIDVar(todoItemId) =>
      val stream = Stream
        .eval(IO(todoItemId))
        .through(todoItemRepository.get)

      Ok(stream)
  }

  def addTodoEntityToRepo(
      validatedTodo: ValidatedNel[ValidatedField, TodoItemEntity]
  ): IO[Either[ValidationError, TodoItemEntity]] = {

    def handleWithError(either: Either[Error, TodoItemEntity]): IO[Either[ValidationError, TodoItemEntity]] = {
      either.fold(
        error => {
          logger.error(
            s"Raising internal server error, could create todo entity due to repository error [error=$error]"
          )
          IO.raiseError(UnexpectedErrorException(s"TodoItemRepository returned error [error=$error]"))
        },
        entity =>
          IO {
            logger.info(s"Created new todo item [todoItemId=${entity.id}]")
            entity.asRight
          }
      )
    }

    validatedTodo.fold(
      validationErrors => IO(createValidationErrorFromValidatedFields(validationErrors)),
      todoItem => todoItemRepository.add(todoItem).flatMap(handleWithError)
    )
  }
}

object TodoStateService {
  implicit val createModelDecoder: EntityDecoder[IO, CreateTodoItemModel] = jsonOf[IO, CreateTodoItemModel]
  implicit val todoItemEncoder: EntityEncoder[IO, TodoItemEntity]         = jsonEncoderOf[IO, TodoItemEntity]

  private def createValidationErrorFromValidatedFields(
      validationErrors: NonEmptyList[ValidatedField]
  ): Either[ValidationError, TodoItemEntity] = {
    ValidationError(validatedFields = validationErrors.toList, errorLocation = BodyError).asLeft
  }
}
