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
import com.github.nscala_time.time.Imports.DateTimeZone
import com.kelvin.whoseturn.config.PostgresqlConfig
import com.kelvin.whoseturn.entities.TodoItemEntity
import com.kelvin.whoseturn.errors.repostiory.PostgreSqlError
import com.kelvin.whoseturn.errors._
import com.typesafe.scalalogging.{CanLog, LazyLogging}
import fs2._
import org.joda.time.DateTime

import java.sql.SQLException
import java.util.UUID

class PostgresqlTodoItemRepository(implicit transactor: Transactor[IO]) extends TodoItemRepository[IO] {
  import PostgresqlTodoItemRepository._

  override def add(todoItemEntity: TodoItemEntity): IO[Either[Error, TodoItemEntity]] = {
    Stream
      .eval(IO(todoItemEntity))
      .through(toInsertQuery)
      .through(executeUpdateQuery)
      .map(_ => todoItemEntity.asRight[Error])
      .compile
      .lastOrError
  }

  override def get: Pipe[IO, UUID, TodoItemEntity] = { stream =>
    stream
      .through(toSelectQuery)
      .through(executeSelectQuery)
  }

  override def update(id: UUID): Pipe[IO, TodoItemEntity, TodoItemEntity] = { stream =>
    stream
      .through(toUpdateQuery)
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

  def resource(postgresqlConfig: PostgresqlConfig): Resource[IO, PostgresqlTodoItemRepository] = {
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
      .map(new PostgresqlTodoItemRepository()(_))
  }

  protected def executeUpdateQuery(
      implicit transactor: Transactor[IO]
  ): Pipe[IO, ConnectionIO[Int], Either[Error, Int]] =
    _.flatMap { con =>
      Stream
        .eval(con.transact(transactor).attemptSql)
        .map(_.leftFlatMap(handleUpdateQueryError))
    }

  protected def toInsertQuery: Pipe[IO, TodoItemEntity, ConnectionIO[Int]] =
    _.map { item =>
      sql"""
          insert into todo_items_items_v1
          (id, title, createdBy, createdOn, lastUpdate, description, flagged, category, priority, location, active)
          values (
                  ${item.id},
                  ${item.title},
                  ${item.createdBy},
                  ${item.createdOn},
                  ${item.lastUpdate},
                  ${item.description},
                  ${item.flagged},
                  ${item.category},
                  ${item.priority},
                  ${item.location},
                  ${item.active}
          )
       """.update.run
    }

  protected def toSelectQuery: Pipe[IO, UUID, ConnectionIO[TodoItemEntity]] =
    _.map { uuid =>
      sql"""
          select id, title, createdBy, createdOn, lastUpdate, description, flagged, category, priority, location, active
          from todo_items_items_v1
          where id = $uuid
        """.query[TodoItemEntity].unique
    }

  protected def executeSelectQuery(
      implicit transactor: Transactor[IO]
  ): Pipe[IO, ConnectionIO[TodoItemEntity], TodoItemEntity] =
    _.flatMap(con => Stream.eval(con.transact(transactor)))

  protected def toUpdateQuery: Pipe[IO, TodoItemEntity, ConnectionIO[Int]] = _.map { entity =>
    val updatedOn = DateTime.now().toDateTime(DateTimeZone.UTC)
    val updatedOnTimestamp = new java.sql.Timestamp(updatedOn.getMillis)

    val query =
      sql"""
        update todo_items_items_v1
          set 
              title=${entity.title},
              lastUpdate=${updatedOnTimestamp},
              description=${entity.description},
              flagged=${entity.flagged},
              category=${entity.category},
              priority=${entity.priority},
              location=${entity.location}
        where id=${entity.id}
        """

    query.update.run
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
