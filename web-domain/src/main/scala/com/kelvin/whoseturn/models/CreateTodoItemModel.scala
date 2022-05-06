package com.kelvin.whoseturn.models

final case class CreateTodoItemModel(
    title: String,
    description: String,
    flagged: Boolean,
    category: String,
    priority: String,
    location: String,
    active: Boolean
)
