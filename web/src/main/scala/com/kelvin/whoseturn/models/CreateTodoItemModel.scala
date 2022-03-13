package com.kelvin.whoseturn.models

import java.sql.Timestamp
import java.util.UUID

final case class CreateTodoItemModel(
    title: String,
    description: String,
    flagged: Boolean,
    category: String,
    priority: String,
    location: String,
    active: Boolean
)
