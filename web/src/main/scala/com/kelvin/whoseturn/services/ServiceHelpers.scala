package com.kelvin.whoseturn.services

import cats._
import cats.implicits._
import cats.data._
import cats.effect.IO
import com.kelvin.whoseturn.entity.TodoItemEntity
import com.kelvin.whoseturn.errors._
import com.kelvin.whoseturn.errors.ValidationError
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

trait ServiceHelpers {
  private val logger: Logger[IO]                                     = Slf4jLogger.getLogger[IO]
  implicit val validationEncoder: EntityEncoder[IO, ValidationError] = jsonEncoderOf[IO, ValidationError]

  def handleIOError(error: Throwable): IO[Response[IO]] = {
    val criticalError = CriticalError(message = "There was an Internal Service Error, please try again.")

    error match {
      case badJsonError: MalformedMessageBodyFailure => createMalformedJsonResponse(badJsonError)
      case err: Throwable                            => createCriticalErrorResponse(err, criticalError)
    }
  }

  def createValidationResponse[T](
      result: Either[ValidationError, T]
  )(implicit encoder: EntityEncoder[IO, T]): IO[Response[IO]] = {
    def handleValidationError(validationError: ValidationError): IO[Response[IO]] = {
      val fields   = validationError.validatedFields.map(_.field).mkString(",")
      val location = validationError.errorLocation

      logger.warn(s"There was a validation error for request [fields=$fields, location=$location]") >> BadRequest(
        validationError
      )
    }

    result.fold(
      validationError => handleValidationError(validationError),
      todoEntity => Ok(todoEntity)
    )
  }

  private def createCriticalErrorResponse(error: Throwable, internalError: CriticalError): IO[Response[IO]] = {
    implicit val encoder: EntityEncoder[IO, CriticalError] = jsonEncoderOf[IO, CriticalError]
    logger.error(error)("An unhandled exception was thrown in a request") >> InternalServerError(internalError)
  }

  private def createMalformedJsonResponse(failure: MalformedMessageBodyFailure): IO[Response[IO]] = {
    logger.warn(failure)("Request contained malformed json and was rejected") >> BadRequest()
  }
}
