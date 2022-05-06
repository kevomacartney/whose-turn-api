package com.kelvin.whoseturn.entities

import java.sql.Timestamp

import java.util.UUID

case class TodoItemEntity(
    id: UUID,
    title: String,
    createdBy: UUID,
    createdOn: Timestamp,
    lastUpdate: Timestamp,
    description: String,
    flagged: Boolean,
    category: String,
    priority: String,
    location: String,
    active: Boolean
)
