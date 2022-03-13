package testSupport

import cats._
import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
import org.scalatest._
import org.testcontainers.containers.PostgreSQLContainer
import cats.effect.unsafe.implicits.global
import com.kelvin.whoseturn.entity.TodoItemEntity
import doobie.util.transactor.Transactor.Aux

import java.util.UUID

trait PostgresqlTestSupport extends Suite with BeforeAndAfterAll {
  implicit val container: PostgreSQLContainer[Nothing] = new PostgreSQLContainer("postgres")

  override def beforeAll(): Unit = {
    container.start()
  }

  override def afterAll(): Unit = {
    container.stop()
  }

  def initialisePostgresqlTable(implicit context: PostgresqlContext): String = {
    implicit val transactor: Transactor[IO] = getTransactor

    executeCreateQuery(context.createScriptPath)
  }

  def getTransactor: Transactor[IO] = {
    val port        = container.getMappedPort(5432)
    val databaseUrl = s"jdbc:postgresql://localhost:$port/test"

    Transactor.fromDriverManager[IO](
      "org.postgresql.Driver", // driver classname
      databaseUrl,             // connect URL (driver-specific)
      "test",                  // user
      "test"                   // password
    )
  }

  private def executeCreateQuery(scriptPath: String)(implicit transactor: Transactor[IO]): String = {
    val source     = scala.io.Source.fromFile(scriptPath)
    val identifier = UUID.randomUUID().toString.replace("-", "")
    val tableName  = s"todo_items_$identifier"

    val queryStr = source.mkString.replace("tableName_", tableName)

    Update(queryStr)
      .toUpdate0()
      .run
      .transact(transactor)
      .unsafeRunSync()

    tableName
  }
}

case class PostgresqlContext(createScriptPath: String, jsonDataPath: String)
