package com.kelvin.whoseturn.support.fixtures

import com.github.nscala_time.time.Imports.DateTimeZone
import com.kelvin.whoseturn.entities._
import com.kelvin.whoseturn.models._
import org.joda.time.DateTime

import java.util.UUID

trait TodoItemsFixtures {
  def CreateTodoItemModelFixture(
      title: String = "Laundry pods",
      description: String = "Ariel laundry pods, 38 box",
      flagged: Boolean = false,
      category: String = "Kitchen",
      priority: String = "low",
      location: String = "London",
      active: Boolean = true
  ): CreateTodoItemModel = {
    CreateTodoItemModel(title, description, flagged, category, priority, location, active)
  }

  def TodoItemEntityFixture(
      id: UUID = UUID.randomUUID(),
      title: String = "Laundry pods",
      createdBy: UUID = UUID.randomUUID(),
      createdOn: DateTime = DateTime.now().toDateTime(DateTimeZone.UTC),
      lastUpdate: DateTime = DateTime.now().toDateTime(DateTimeZone.UTC),
      description: String = "Ariel laundry pods, 38 box",
      flagged: Boolean = false,
      category: String = "Kitchen",
      priority: String = "low",
      location: String = "London",
      active: Boolean = true
  ): TodoItemEntity =
    TodoItemEntity(
      id = id,
      title = title,
      createdBy = createdBy,
      createdOn = new java.sql.Timestamp(createdOn.getMillis),
      lastUpdate = new java.sql.Timestamp(lastUpdate.getMillis),
      description = description,
      flagged = flagged,
      category = category,
      priority = priority,
      location = location,
      active = active
    )
}
