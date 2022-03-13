package com.kelvin.whoseturn.repositories

import cats.data.EitherT
import cats.effect.IO
import com.kelvin.whoseturn.entity.TodoItemEntity
import com.kelvin.whoseturn.errors.RepositoryError
import fs2.Pipe

import java.util.UUID

trait TodoItemRepository[F[_]] {
  def getItem(todoItemEntity: TodoItemEntity): Pipe[IO, UUID, TodoItemEntity]

  def addItem(todoItemEntity: TodoItemEntity): IO[TodoItemEntity]

  def updateItem(id: UUID): Pipe[IO, TodoItemEntity, TodoItemEntity]
}
