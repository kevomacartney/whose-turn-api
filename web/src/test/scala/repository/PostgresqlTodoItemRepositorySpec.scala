package repository

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.kelvin.whoseturn.entity.TodoItemEntity
import com.kelvin.whoseturn.repositories.PostgresqlTodoItemRepository
import doobie._
import doobie.implicits._
import doobie.implicits.javasql._
import doobie.postgres._
import doobie.postgres.implicits._
import doobie.postgres.pgisimplicits._
import fs2._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import testSupport._

import java.util.UUID

class PostgresqlTodoItemRepositorySpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with TodoItemsFixtures
    with PostgresqlTestSupport {
  import PostgresqlTodoItemRepositorySpec._

  "PostgresqlTodoItemRepository" should {
    "write todo items to the database" in {
      val table                               = initialisePostgresqlTable
      implicit val transactor: Transactor[IO] = getTransactor

      withPostgresqlTodoItemRepository(table) { repo =>
        val writtenEntity = TodoItemEntityFixture().copy(id = UUID.randomUUID())

        repo.addItem(writtenEntity).unsafeRunSync()

        val entity: TodoItemEntity = readTodoItemById(entityId = writtenEntity.id, tableName = table)

        entity mustBe writtenEntity
      }
    }

    "read todo items from the database" in {
      val table                               = initialisePostgresqlTable
      implicit val transactor: Transactor[IO] = getTransactor

      withPostgresqlTodoItemRepository(table) { repo =>
        val writtenEntity = TodoItemEntityFixture().copy(id = UUID.randomUUID())
        addTodoItem(writtenEntity, table)

        val getResult = Stream
          .eval(IO(writtenEntity.id))
          .through(repo.getItem)
          .compile
          .last
          .unsafeRunSync()
          .get

        getResult mustBe writtenEntity
      }
    }

    "update existing todo items in database" in {
      val table                               = initialisePostgresqlTable
      implicit val transactor: Transactor[IO] = getTransactor

      withPostgresqlTodoItemRepository(table) { repo =>
        val writtenEntity = TodoItemEntityFixture().copy(id = UUID.randomUUID())
        val updatedEntity = writtenEntity.copy(description = s"updated description ${UUID.randomUUID()}")

        addTodoItem(writtenEntity, table)

        Stream
          .eval(IO(updatedEntity))
          .through(repo.updateItem(writtenEntity.id))
          .compile
          .last
          .unsafeRunSync()
          .get

        val getResult = readTodoItemById(writtenEntity.id, table)

        getResult mustBe updatedEntity
      }
    }
  }
}

object PostgresqlTodoItemRepositorySpec {
  implicit val context: PostgresqlContext = {
    val scriptPath   = getClass.getResource("/create_todo_items.sql").getPath
    val jsonDataPath = getClass.getResource("/todo_items_dump.json").getPath

    PostgresqlContext(scriptPath, jsonDataPath)
  }

  def withPostgresqlTodoItemRepository[T](tableName: String)(f: PostgresqlTodoItemRepository => T)(
      implicit transactor: Transactor[IO]
  ): T = {
    val repo = new PostgresqlTodoItemRepository(tableName, transactor)

    f(repo)
  }

  def readTodoItemById(entityId: UUID, tableName: String)(implicit transactor: Transactor[IO]): TodoItemEntity = {
    val query =
      s"""
          select id, title, createdBy, createdOn, lastUpdate, description, flagged, category, priority, location, active
          from $tableName
          where id = ?
        """

    Query[UUID, TodoItemEntity](query)
      .toQuery0(entityId)
      .unique
      .transact(transactor)
      .unsafeRunSync()
  }

  def addTodoItem(entity: TodoItemEntity, tableName: String)(implicit transactor: Transactor[IO]): Unit = {
    val sql =
      s"""insert
         |  into $tableName
         |(id, title, createdBy, createdOn, lastUpdate, description, flagged, category,priority, location, active) 
         |  values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""".stripMargin

    Update[TodoItemEntity](sql)
      .toUpdate0(entity)
      .run
      .transact(transactor)
      .unsafeRunSync()
  }
}
