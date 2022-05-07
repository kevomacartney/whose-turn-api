package com.kelvin.whoseturn.kafka.messages.Codecs

import cats.implicits._
import com.kelvin.whoseturn.kafka.messages.TodoItemUserActionEvent
import vulcan.Codec

object WhoseTurnTodoItemEventCodec {
  implicit val whoseTurnTodoItemEventCodec: Codec[TodoItemUserActionEvent] =
    Codec.record[TodoItemUserActionEvent](
      name = "TodoItemUserActionEvent",
      namespace = "com.kelvin.whoseturn.events.user"
    ) { field =>
      (
        field("todoId", _.todoId),
        field("action", _.action),
        field("triggeredBy", _.triggeredBy),
        field("delayedTo", _.delayedTo),
        field("requestedTimestamp", _.requestedTimestamp),
        field("source", _.source),
        field("attributes", _.attributes)
      ).mapN(TodoItemUserActionEvent)
    }
}
