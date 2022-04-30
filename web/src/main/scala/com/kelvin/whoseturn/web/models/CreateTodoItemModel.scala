package com.kelvin.whoseturn.web.models

import com.kelvin.whoseturn.todo.Priority

import java.sql.Timestamp
import java.util.UUID

final case class CreateTodoItemModel(
    title: String,
    description: String,
    flagged: Boolean,
    category: String,
    priority: Priority,
    location: String,
    active: Boolean
)
