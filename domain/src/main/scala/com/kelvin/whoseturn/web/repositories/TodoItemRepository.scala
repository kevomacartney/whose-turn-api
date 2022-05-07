package com.kelvin.whoseturn.web.repositories

import cats.data._
import cats.effect.IO
import com.kelvin.whoseturn.entities.TodoItemEntity
import com.kelvin.whoseturn.errors.Error
import com.kelvin.whoseturn.repositories.GenericRepository
import fs2.Pipe

import java.util.UUID

trait TodoItemRepository[F[_]] extends GenericRepository[F, TodoItemEntity] {
  def get: Pipe[F, UUID, TodoItemEntity]

  def add(todoItemEntity: TodoItemEntity): F[Either[Error, TodoItemEntity]]

  def update(id: UUID): Pipe[F, TodoItemEntity, TodoItemEntity]
}
