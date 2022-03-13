package com.kelvin.whoseturn.cassandra

import cats.effect.unsafe.implicits.global
import cats.effect.{Async, IO}
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.{BoundStatement, Row, SimpleStatement}
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.{bindMarker, selectFrom}
import com.kelvin.whoseturn.entity.TodoItemEntity
import fs2._
import io.circe.fs2._
import io.circe.generic.auto._

object CassandraQuerySupport {

  def queryEntityWithIdBind(table: String): SimpleStatement = {
    selectFrom("whose_turn", table)
      .columns(
        "id",
        "title",
        "createdBy",
        "createdOn",
        "lastUpdate",
        "description",
        "flagged",
        "category",
        "priority",
        "location",
        "active"
      )
      .whereColumn("id")
      .isEqualTo(bindMarker())
      .build()
  }

  def toTodoItemEntity: Iterator[Row] => IO[List[TodoItemEntity]] = ???

//  { it =>
//    toFs2Stream[Row, IO](it)
//      .through(cassandraRowToJson)
//      .through(stringArrayParser)
//      .through(decoder[IO, TodoItemEntity])
//      .compile
//      .toList
//  }

  private def cassandraRowToJson: Pipe[IO, Row, String] = rows => rows.map(a => a.getString(0))

  def toFs2Stream[A, F[_] >: Pure[_]](iter: Iterator[A]): Stream[F, A] =
    Stream.unfold[F, Iterator[A], A](iter)(i => if (i.hasNext) Some((i.next, i)) else None)
}
