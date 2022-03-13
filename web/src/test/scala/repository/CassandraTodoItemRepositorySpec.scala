package repository

import cats.effect._
import cats._
import cats.effect.unsafe.implicits.global
import cats.implicits._
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.metadata.EndPoint
import com.kelvin.whoseturn.entity.TodoItemEntity
import com.kelvin.whoseturn.repositories.CassandraTodoItemRepository
import io.circe.Decoder
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.testcontainers.containers.CassandraContainer
import repository.CassandraTodoItemRepositorySpec.{withCassandraSession, withCassandraTodoItemRepository}
import testSupport.{CassandraContext, CassandraSupport, TodoItemsFixtures}
import fs2._

import java.net.InetSocketAddress

class CassandraTodoItemRepositorySpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with CassandraSupport
    with TodoItemsFixtures {
  "CassandraTodoItemRepository" should {
    "successfully write items to cassandra" in {
      implicit val context: CassandraContext =
        CassandraContext(createScriptPath = CreateTodoItemsTableScriptPath, jsonDataPath = TodoItemsJsonDataPath)

      withCassandraSession { implicit session =>
        val tableName = initialiseCassandraTable
        withCassandraTodoItemRepository(tableName) { repo =>
          val todoItemEntity = TodoItemEntityFixture()

          Stream
            .eval(IO(todoItemEntity))
            .through(repo.addItem)
            .compile
            .last
            .unsafeRunSync()

          val entities = entityTodoItem(tableName, todoItemEntity.id)
          print("inshallah")
        }
      }
    }
  }
}

object CassandraTodoItemRepositorySpec {
  def withCassandraSession[T](f: CqlSession => T)(implicit container: CassandraContainer[Nothing]): T = {
    val port = container.getMappedPort(9042)

    val session = CqlSession
      .builder()
      .addContactPoint(new InetSocketAddress("127.0.0.1", port))
      .withLocalDatacenter("datacenter1")
      .build()

    f(session)
  }

  def withCassandraTodoItemRepository[T](
      tableName: String
  )(f: CassandraTodoItemRepository => T)(implicit session: CqlSession): T = {
    val repo = new CassandraTodoItemRepository(tableName, session)
    f(repo)
  }
}
