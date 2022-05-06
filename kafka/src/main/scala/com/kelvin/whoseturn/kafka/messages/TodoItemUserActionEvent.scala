package com.kelvin.whoseturn.kafka.messages

import java.time.Instant
import java.util.UUID

object EventAction extends Enumeration {
  val COMPLETED, SKIPPED, DELAYED, DELETED = Value
}

case class TodoItemUserActionEvent(
    todoId: UUID,
    action: String,
    triggeredBy: UUID,
    delayedTo: Option[Instant],
    requestedTimestamp: Instant,
    source: String,
    attributes: Map[String, String]
)
