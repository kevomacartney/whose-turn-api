package fixtures

import com.kelvin.whoseturn.kafka.messages._

import java.time.Instant
import java.util.UUID

trait TodoItemUserActionEventFixture {
  val TodoItemTodoId: UUID = UUID.fromString("7d94e2be-2d32-4d32-9ca4-60da2ac1b5e7")

  val TodoItemEventRequestedTimestamp: Instant = Instant.ofEpochMilli(1650624254314L)
  val TodoItemEventDelayTimestamp: Instant     = Instant.ofEpochMilli(1650624454314L)

  def WhoseTurnTodoItemEventFixture(
      todoId: UUID = TodoItemTodoId,
      action: EventAction.Value = EventAction.COMPLETED,
      requestedTimestamp: Instant = TodoItemEventRequestedTimestamp,
      source: String = "test",
      delayedTo: Option[Instant] = Some(TodoItemEventDelayTimestamp),
      attributes: Map[String, String] = Map()
  ): TodoItemUserActionEvent = {
    TodoItemUserActionEvent(
      todoId = todoId,
      action = action.toString,
      triggeredBy = UUID.randomUUID(),
      delayedTo = delayedTo,
      requestedTimestamp = requestedTimestamp,
      source = source,
      attributes = attributes
    )
  }

}
