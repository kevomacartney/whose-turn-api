package repository

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.github.nscala_time.time.Imports.DateTimeZone
import com.kelvin.whoseturn.entity.TodoItemEntity
import com.kelvin.whoseturn.repositories.PostgresqlTodoItemRepository
import com.kelvin.whoseturn.support.test.postgresql._
import doobie._
import doobie.implicits._
import doobie.implicits.javasql._
import doobie.postgres._
import doobie.postgres.implicits._
import doobie.postgres.pgisimplicits._
import fs2._
import org.joda.time.DateTime
import org.scalatest.Inside
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
    with PostgresqlTestSupport
    with Inside {
  import PostgresqlTodoItemRepositorySpec._

  "PostgresqlTodoItemRepository" should {
    "write todo items to the database" in {
      withPostgresql { implicit transactor =>
        withPostgresqlTodoItemRepository { repo =>
          val writtenEntity = TodoItemEntityFixture().copy(id = UUID.randomUUID())

          repo.add(writtenEntity).unsafeRunSync()

          val entity: TodoItemEntity = getTodoItemById(entityId = writtenEntity.id)

          entity mustBe writtenEntity
        }
      }
    }

    "read todo items from the database" in {
      withPostgresql { implicit transactor =>
        withPostgresqlTodoItemRepository { repo =>
          val writtenEntity = TodoItemEntityFixture().copy(id = UUID.randomUUID())
          addTodoItem(writtenEntity)

          val getResult = Stream
            .eval(IO(writtenEntity.id))
            .through(repo.get)
            .compile
            .last
            .unsafeRunSync()
            .get

          getResult mustBe writtenEntity
        }
      }
    }

    "update existing todo item in database" in {
      withPostgresql { implicit transactor =>
        withPostgresqlTodoItemRepository { repo =>
          val writtenEntity = TodoItemEntityFixture().copy(id = UUID.randomUUID())
          val updatedEntity = writtenEntity.copy(
            title = UUID.randomUUID().toString,
            description = UUID.randomUUID().toString,
            flagged = !writtenEntity.flagged,
            category = UUID.randomUUID().toString,
            priority = UUID.randomUUID().toString,
            location = UUID.randomUUID().toString,
            active = writtenEntity.active
          )

          addTodoItem(writtenEntity)

          Stream
            .eval(IO(updatedEntity))
            .through(repo.update(writtenEntity.id))
            .compile
            .last
            .unsafeRunSync()

          val getResult = getTodoItemById(writtenEntity.id)

          inside(getResult) {
            case TodoItemEntity(_, title, _, _, lastUpdate, desc, flagged, category, priority, location, active) =>
              lastUpdate must not equal writtenEntity.lastUpdate
              title mustBe updatedEntity.title
              desc mustBe updatedEntity.description
              flagged mustBe updatedEntity.flagged
              category mustBe updatedEntity.category
              priority mustBe updatedEntity.priority
              location mustBe updatedEntity.location
          }
        }
      }
    }

    "does not update readonly fields (createdBy, createdOn)" in {
      withPostgresql { implicit transactor =>
        withPostgresqlTodoItemRepository { repo =>
          val writtenEntity = TodoItemEntityFixture().copy(id = UUID.randomUUID())
          val updatedEntity = writtenEntity.copy(
            createdBy = UUID.randomUUID(),
            createdOn = new java.sql.Timestamp(DateTime.now().getMillis)
          )

          addTodoItem(writtenEntity)

          Stream
            .eval(IO(updatedEntity))
            .through(repo.update(writtenEntity.id))
            .compile
            .last
            .unsafeRunSync()

          val getResult = getTodoItemById(writtenEntity.id)

          getResult must not equal updatedEntity
        }
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

  def withPostgresqlTodoItemRepository[T](f: PostgresqlTodoItemRepository => T)(
      implicit transactor: Transactor[IO]
  ): T = {
    val repo = new PostgresqlTodoItemRepository()(transactor)

    f(repo)
  }

  def getTodoItemById(entityId: UUID)(implicit transactor: Transactor[IO]): TodoItemEntity = {
    val query =
      s"""
          select id, title, createdBy, createdOn, lastUpdate, description, flagged, category, priority, location, active
          from todo_items_items_v1
          where id = ?
        """

    Query[UUID, TodoItemEntity](query)
      .toQuery0(entityId)
      .unique
      .transact(transactor)
      .unsafeRunSync()
  }

  def addTodoItem(entity: TodoItemEntity)(implicit transactor: Transactor[IO]): Unit = {
    val sql =
      s"""insert
         |  into todo_items_items_v1
         |(id, title, createdBy, createdOn, lastUpdate, description, flagged, category,priority, location, active) 
         |  values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""".stripMargin

    Update[TodoItemEntity](sql)
      .toUpdate0(entity)
      .run
      .transact(transactor)
      .unsafeRunSync()
  }
}
