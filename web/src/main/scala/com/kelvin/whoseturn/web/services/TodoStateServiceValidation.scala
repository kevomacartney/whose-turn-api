package com.kelvin.whoseturn.web.services

import cats._
import cats.data._
import cats.implicits._
import cats.syntax._
import com.kelvin.whoseturn.entity.TodoItemEntity
import com.kelvin.whoseturn.errors.http.ValidatedField
import com.kelvin.whoseturn.implicits.TimestampImplicits._
import com.kelvin.whoseturn.web.models.CreateTodoItemModel
import org.joda.time.DateTime

import java.util.UUID

object TodoStateServiceValidation {
  def validateCreateTodoItemModel(model: CreateTodoItemModel): ValidatedNel[ValidatedField, TodoItemEntity] = {
    val validatedTitle    = validateTitle(model.title)
    val validatedLocation = validateLocation(model.location)

    (validatedTitle, validatedLocation).mapN((title, location) => {
      TodoItemEntity(
        id = UUID.randomUUID(),
        title = title,
        createdBy = UUID.randomUUID(),
        createdOn = DateTime.now().toTimestamp,
        lastUpdate = DateTime.now().toTimestamp,
        description = model.description,
        flagged = model.flagged,
        category = model.category,
        priority = model.priority.toString,
        location = location,
        active = model.active
      )
    })
  }

  private def validateLocation(location: String): ValidatedNel[ValidatedField, String] = {
    if (location.nonEmpty)
      return location.validNel[ValidatedField]

    Validated.invalidNel(createRequiredFieldError("location"))
  }

  private def validateTitle(title: String): ValidatedNel[ValidatedField, String] = {
    if (title.nonEmpty)
      return title.validNel[ValidatedField]

    Validated.invalidNel(createRequiredFieldError("title"))
  }

  private def createRequiredFieldError(field: String): ValidatedField =
    ValidatedField(field = field, message = s"$field is a required field")
}
