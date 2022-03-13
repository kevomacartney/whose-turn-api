package com.kelvin.whoseturn.repositories

import cats.data._
import cats.implicits._
import cats.effect.IO
import com.datastax.oss.driver.api.core.cql.ResultSet
import com.datastax.oss.driver.api.core.servererrors.{QueryExecutionException, QueryValidationException}
import com.datastax.oss.driver.api.core.{AllNodesFailedException, CqlSession}
import com.datastax.oss.driver.api.querybuilder.BuildableQuery
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.insertInto
import com.datastax.oss.driver.api.querybuilder.insert.JsonInsert
import com.kelvin.whoseturn.entity.TodoItemEntity
import com.kelvin.whoseturn.errors._
import com.kelvin.whoseturn.repositories.CassandraTodoItemRepository.executeQuery
import com.typesafe.scalalogging.LazyLogging
import fs2.Pipe
import io.circe.generic.auto._
import io.circe.syntax._

import java.util.UUID
import scala.util.Try

class CassandraTodoItemRepository(tableName: String, implicit val session: CqlSession) extends TodoItemRepository[IO] {
//  override def addItem(item: TodoItemEntity): EitherT[IO, RepositoryError, Unit] = {
//    val ioResult = for {
//      json        <- IO(item.asJson.noSpaces)
//      query       <- createInsertQuery(json)
//      queryResult <- executeQuery(query)
//    } yield queryResult
//
//    EitherT(ioResult)
//  }

//  private def createInsertQuery(json: String): IO[JsonInsert] = {
//    IO(insertInto("whose_turn", tableName).json(json))
//  }

  override def addItem: Pipe[IO, TodoItemEntity, Int] = ???

  override def updateItem(id: UUID): Pipe[IO, TodoItemEntity, TodoItemEntity] = ???

  override def getItem: Pipe[IO, UUID, TodoItemEntity] = ???
}

object CassandraTodoItemRepository extends LazyLogging {
  def executeQuery(query: BuildableQuery)(implicit session: CqlSession): IO[Either[RepositoryError, Unit]] = {
    IO {
      val built = query.build()
      Either
        .fromTry(Try(session.execute(built)))
        .map(_ => ())
        .leftMap { error =>
          logger.info(s"There was an error while executing cassandra query [error=${error.getMessage}]", error)
          mapThrowableToRepositoryError(error)
        }
    }
  }

  def mapThrowableToRepositoryError(throwable: Throwable): RepositoryError = {
    throwable match {
      case _: AllNodesFailedException  => DatabaseOffline
      case _: QueryExecutionException  => DatabaseError
      case _: QueryValidationException => QueryInvalid
      case _                           => Failure
    }
  }
}
