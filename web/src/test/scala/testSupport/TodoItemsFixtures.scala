package testSupport

import com.github.nscala_time.time.Imports.DateTimeZone
import com.kelvin.whoseturn.entity.TodoItemEntity
import org.joda.time.DateTime

import java.util.UUID

trait TodoItemsFixtures {
  val CreateTodoItemsTableScriptPath: String = getClass.getResource("/create_todo_items.cql").getPath
  val TodoItemsJsonDataPath: String          = getClass.getResource("/todo_items_dump.json").getPath

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
      active: Boolean = false
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
