package com.kelvin.whoseturn.repositories

import doobie._
import doobie.implicits._
import doobie.implicits.javasql._
import doobie.postgres._
import doobie.postgres.implicits._
import doobie.postgres.pgisimplicits._
import cats.data._
import cats.syntax._
import cats.implicits._
import cats.effect._
import com.kelvin.whoseturn.config.PostgresqlConfig
import com.kelvin.whoseturn.entity.TodoItemEntity
import com.kelvin.whoseturn.errors.repostiory.PostgreSqlError
import com.kelvin.whoseturn.errors._
import com.typesafe.scalalogging.{CanLog, LazyLogging}
import fs2._

import java.sql.SQLException
import java.util.UUID

class PostgresqlTodoItemRepository(tableName: String, implicit val transactor: Transactor[IO])
    extends TodoItemRepository[IO] {
  import PostgresqlTodoItemRepository._

  override def add(todoItemEntity: TodoItemEntity): IO[Either[Error, TodoItemEntity]] = {
    Stream
      .eval(IO(todoItemEntity))
      .through(toInsertQuery(tableName))
      .through(executeUpdateQuery)
      .map(_ => todoItemEntity.asRight[Error])
      .compile
      .lastOrError
  }

  override def get: Pipe[IO, UUID, TodoItemEntity] = { stream =>
    stream
      .through(toSelectQuery(tableName))
      .through(executeSelectQuery)
  }

  override def update(id: UUID): Pipe[IO, TodoItemEntity, TodoItemEntity] = { stream =>
    stream
      .through(toUpdateQuery(tableName))
      .through(executeUpdateQuery)
      .map(_ => id)
      .through(get)
  }
}

object PostgresqlTodoItemRepository extends LazyLogging {
  case class CorrelationId(value: String)
  implicit case object CanLogCorrelationId extends CanLog[CorrelationId] {
    override def logMessage(originalMsg: String, a: CorrelationId): String = s"${a.value} $originalMsg"
  }

  def apply(postgresqlConfig: PostgresqlConfig): Resource[IO, PostgresqlTodoItemRepository] = {
    val acquire = IO {
      val databaseUrl = s"jdbc:postgresql://localhost:${postgresqlConfig.port}/test"

      Transactor.fromDriverManager[IO](
        driver = "org.postgresql.Driver",
        url = databaseUrl,
        user = postgresqlConfig.username,
        pass = postgresqlConfig.password
      )
    }

    val release = (_: Transactor[IO]) => IO.unit

    Resource
      .make(acquire)(release)
      .map(new PostgresqlTodoItemRepository(postgresqlConfig.table, _))
  }

  protected def executeUpdateQuery(
      implicit transactor: Transactor[IO]
  ): Pipe[IO, ConnectionIO[Int], Either[Error, Int]] =
    _.flatMap { con =>
      Stream
        .eval(con.transact(transactor).attemptSql)
        .map(_.leftFlatMap(handleUpdateQueryError))
    }

  protected def toInsertQuery(tableName: String): Pipe[IO, TodoItemEntity, ConnectionIO[Int]] =
    _.map { item =>
      val query =
        s"""
          insert into $tableName
          (id, title, createdBy, createdOn, lastUpdate, description, flagged, category,priority, location, active)
          values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
       """
      Update[TodoItemEntity](query).toUpdate0(item).run
    }

  protected def toSelectQuery(tableName: String): Pipe[IO, UUID, ConnectionIO[TodoItemEntity]] =
    _.map { uuid =>
      val query =
        s"""
          select id, title, createdBy, createdOn, lastUpdate, description, flagged, category, priority, location, active
          from $tableName
          where id = ?
        """

      Query[UUID, TodoItemEntity](query)
        .toQuery0(uuid)
        .unique
    }

  protected def executeSelectQuery(
      implicit transactor: Transactor[IO]
  ): Pipe[IO, ConnectionIO[TodoItemEntity], TodoItemEntity] =
    _.flatMap(con => Stream.eval(con.transact(transactor)))

  protected def toUpdateQuery(tableName: String): Pipe[IO, TodoItemEntity, ConnectionIO[Int]] = _.map { entity =>
    val query =
      s"""
        update $tableName
          set title=?, createdBy=?, createdOn=?, lastUpdate=?, description=?, flagged=?, category=?, priority=?, location=?, active=?
        where id=?
        """

    Update[TodoItemEntity](query)
      .toUpdate0(entity)
      .run
  }

  private def handleUpdateQueryError[T](sqlException: SQLException): Either[Error, T] = {
    logger.error(
      s"There was an error while executing update query on todo item [sqlState=${sqlException.getSQLState}]",
      sqlException
    )

    PostgreSqlError(
      cause = DatabaseError,
      message = "Could not update update entity due to unexpected error"
    ).asLeft
  }
}
