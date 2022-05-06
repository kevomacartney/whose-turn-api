package com.kelvin.whoseturn.services

import cats.syntax._
import cats.implicits._
import cats.data._
import com.codahale.metrics.MetricRegistry
import com.kelvin.whoseturn.repositories.TodoItemRepository
import cats.effect.IO
import com.codahale.metrics.MetricRegistry
import com.kelvin.whoseturn.entities.TodoItemEntity
import com.kelvin.whoseturn.errors.{ValidatedField, ValidationError}
import com.kelvin.whoseturn.models.CreateTodoItemModel
import com.typesafe.scalalogging.LazyLogging
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import fs2._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class TodoStateService(todoItemRepository: TodoItemRepository[IO])(implicit var metricRegistry: MetricRegistry)
    extends ServicePipes {
  import TodoStateService._

  private val logger: Logger[IO]                               = Slf4jLogger.getLogger[IO]
  implicit val decoder: EntityDecoder[IO, CreateTodoItemModel] = jsonOf[IO, CreateTodoItemModel]

  def add(): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ PUT -> Root / "add" =>
      req
        .as[CreateTodoItemModel]
        .map(applyValidation)

      Ok("")
  }

  def applyValidation(createdTodoModel: CreateTodoItemModel): ValidatedNel[ValidationError, TodoItemEntity] = ???

  def applyToRepository(validatedModel: ValidatedNel[ValidatedField, TodoItemEntity]) = ???

  def addTodoEntityToRepo
      : Pipe[IO, ValidatedNel[ValidationError, TodoItemEntity], ValidatedNel[ValidatedField, TodoItemEntity]] =
    stream => ???

  def handleError(error: Throwable): Stream[IO, Json] = ???
}

object TodoStateService {
  private val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private def createValidationResponse(validationError: ValidationError): IO[Response[IO]] = {
    implicit val encoder: EntityEncoder[IO, ValidationError] = jsonEncoderOf[IO, ValidationError]

    val fields   = validationError.validatedFields.map(_.field).mkString(",")
    val location = validationError.errorLocation
    logger.warn(s"There was a validation error for request [fields=$fields, location=$location]") >> BadRequest(
      validationError
    )
  }
}
