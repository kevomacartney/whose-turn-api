package com.kelvin.whoseturn.web.repositories

import cats.data._
import cats.effect.IO
import com.kelvin.whoseturn.entity.TodoItemEntity
import com.kelvin.whoseturn.errors.RepositoryError
import fs2.Pipe

import java.util.UUID

trait TodoItemRepository[F[_]] {
  def getItem: Pipe[IO, UUID, TodoItemEntity]

  def addItem(todoItemEntity: TodoItemEntity): IO[TodoItemEntity]

  def updateItem(id: UUID): Pipe[IO, TodoItemEntity, TodoItemEntity]
}
