package com.kelvin.whoseturn.services

import cats.syntax._
import cats.implicits._
import cats.data._
import com.codahale.metrics.MetricRegistry
import com.kelvin.whoseturn.repositories.TodoItemRepository
import cats.effect.IO
import com.codahale.metrics.MetricRegistry
import com.kelvin.whoseturn.entity.TodoItemEntity
import com.kelvin.whoseturn.errors.meta.BodyError
import com.kelvin.whoseturn.errors.{ValidatedField, ValidationError}
import com.kelvin.whoseturn.models.CreateTodoItemModel
import com.kelvin.whoseturn.implicits.CirceTimestampEncoder._
import com.kelvin.whoseturn.implicits.TimestampImplicits._
import com.typesafe.scalalogging.LazyLogging
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import fs2._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.joda.time.DateTime
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.sql.Timestamp
import java.util.UUID

class TodoStateService(implicit var metricRegistry: MetricRegistry, todoItemRepository: TodoItemRepository[IO])
    extends ServiceHelpers {

  import TodoStateService._

  def add(): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ PUT -> Root / "add" =>
      req
        .as[CreateTodoItemModel]
        .map(applyValidation)
        .flatMap(addTodoEntityToRepo)
        .flatMap(createValidationResponse[TodoItemEntity])
        .handleErrorWith(handleIOError)
  }

  def applyValidation(createdTodoModel: CreateTodoItemModel): ValidatedNel[ValidatedField, TodoItemEntity] =
    createdTodoModel.validNel[ValidatedField].map { model =>
      TodoItemEntity(
        id = UUID.randomUUID(),
        title = model.title,
        createdBy = UUID.randomUUID(),
        createdOn = DateTime.now().toTimestamp,
        lastUpdate = DateTime.now().toTimestamp,
        description = model.description,
        flagged = model.flagged,
        category = model.category,
        priority = model.priority,
        location = model.location,
        active = model.active
      )
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
