package com.kelvin.whoseturn.services

import cats._
import cats.implicits._
import cats.data._
import cats.effect.IO
import com.kelvin.whoseturn.entity.TodoItemEntity
import com.kelvin.whoseturn.errors._
import com.kelvin.whoseturn.errors.ValidationError
import com.kelvin.whoseturn.models.CreateTodoItemModel
import fs2.Pipe
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe.jsonOf
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import fs2._
trait ServicePipes {
  import ServicePipes._

  private val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def createResponse[T](
      response: Option[ResponsePayload[T]]
  )(implicit encoder: EntityEncoder[IO, T]): IO[Response[IO]] = {
    response.fold(handleFailedStream) {
      case Left(error)            => handleError(error)
      case Right(responsePayload) => Ok(responsePayload)
    }
  }

  private def handleFailedStream: IO[Response[IO]] = {
    InternalServerError()
  }

  private def handleError(err: Error): IO[Response[IO]] = {
    err match {
      case validationErr: ValidationError => createValidationResponse(validationErr)
      case internalErr: CriticalError     => createCriticalErrorResponse(internalErr)
      case unknownError: Error            => unmatchedErrorResponse(unknownError)
    }
  }

  private def unmatchedErrorResponse(unknownError: Error): IO[Response[IO]] = {
    val error: CriticalError                               = CriticalError(message = "There was a critical error, please try again shortly")
    implicit val encoder: EntityEncoder[IO, CriticalError] = jsonEncoderOf[IO, CriticalError]

    val typeName = unknownError.getClass.getTypeName
    logger.error(s"There was an unknown error thrown by endpoint [type=$typeName]") >> InternalServerError(error)
  }

  private def createValidationResponse(validationErr: ValidationError): IO[Response[IO]] = {
    implicit val encoder: EntityEncoder[IO, ValidationError] = jsonEncoderOf[IO, ValidationError]
    logger.warn(
      s"There was a validation error for request [fields=${validationErr.validatedFields.mkString(",")}, location=${validationErr.errorLocation}]"
    ) >> BadRequest(validationErr)
  }

  private def createCriticalErrorResponse(internalError: CriticalError): IO[Response[IO]] = {
    implicit val encoder: EntityEncoder[IO, CriticalError] = jsonEncoderOf[IO, CriticalError]
    logger.error("There was an internal server error for request") >> InternalServerError(internalError)
  }
}

object ServicePipes {
  type ResponsePayload[T] = Either[Error, T]
}
