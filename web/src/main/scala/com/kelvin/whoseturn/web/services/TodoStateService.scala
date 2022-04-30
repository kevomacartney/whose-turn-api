package com.kelvin.whoseturn.web.services

import cats.syntax._
import cats.implicits._
import cats.data._
import com.codahale.metrics.MetricRegistry
import com.kelvin.whoseturn.web.repositories.TodoItemRepository
import cats.effect.IO
import com.kelvin.whoseturn.entity.TodoItemEntity
import com.kelvin.whoseturn.errors.meta.BodyError
import com.kelvin.whoseturn.errors.http._
import com.kelvin.whoseturn.web.models.CreateTodoItemModel
import com.kelvin.whoseturn.todo.Priority._
import com.kelvin.whoseturn.implicits.CirceTimestampEncoder._
import com.kelvin.whoseturn.implicits.TimestampImplicits._
import com.kelvin.whoseturn.web.services.TodoStateServiceValidation._
import com.typesafe.scalalogging.LazyLogging
import fs2.Stream
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.joda.time.DateTime

import java.util.UUID

class TodoStateService(implicit var metricRegistry: MetricRegistry, todoItemRepository: TodoItemRepository[IO])
    extends ServiceHelpers {

  import TodoStateService._

  def add(): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ PUT -> Root / "add" =>
      req
        .as[CreateTodoItemModel]
        .map(validateCreateTodoItemModel)
        .flatMap(addTodoEntityToRepo)
        .flatMap(createValidationResponse[TodoItemEntity])
        .handleErrorWith(handleIOError)
  }

  def get(): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "get" / UUIDVar(todoItemId) =>
      val stream = Stream
        .eval(IO(todoItemId))
        .through(todoItemRepository.getItem)

      Ok(stream)
  }

  def addTodoEntityToRepo(
      validatedTodo: ValidatedNel[ValidatedField, TodoItemEntity]
  ): IO[Either[ValidationError, TodoItemEntity]] = {
    validatedTodo.fold(
      validationErrors => IO(createValidationErrorFromValidatedFields(validationErrors)),
      todoItem => todoItemRepository.addItem(todoItem).map(_.asRight[ValidationError])
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
