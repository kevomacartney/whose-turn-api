package com.kelvin.whoseturn.repositories

import cats.data.EitherT
import cats.effect.IO
import com.kelvin.whoseturn.entity.TodoItemEntity
import com.kelvin.whoseturn.errors.Error
import fs2.Pipe

import java.util.UUID

trait TodoItemRepository[F[_]] extends GenericRepository[F, TodoItemEntity] {
  def get: Pipe[F, UUID, TodoItemEntity]

  def add(todoItemEntity: TodoItemEntity): F[Either[Error, TodoItemEntity]]

  def update(id: UUID): Pipe[F, TodoItemEntity, TodoItemEntity]
}
