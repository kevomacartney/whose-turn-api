package com.kelvin.whoseturn.web.test.testSupport

import cats.effect.unsafe.implicits.global
import cats.effect._
import com.datastax.driver.core.Session
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.{bindMarker, insertInto, selectFrom}
import com.datastax.oss.driver.api.core.CqlSession
import com.kelvin.whoseturn.cassandra.CassandraQuerySupport.queryEntityWithIdBind
import com.kelvin.whoseturn.entity.TodoItemEntity
import org.scalatest.{BeforeAndAfterAll, Suite}
import org.testcontainers.containers.CassandraContainer
import io.circe._
import io.circe.parser._

import java.util.UUID
import collection.JavaConverters._
import io.circe.fs2._
case class CassandraContext(createScriptPath: String, jsonDataPath: String)
import com.kelvin.whoseturn.cassandra.CassandraQuerySupport._

trait CassandraSupport extends Suite with BeforeAndAfterAll {
  implicit val container: CassandraContainer[Nothing] = new CassandraContainer("cassandra")

  override def beforeAll(): Unit = {
    container.start()
  }

  def initialiseCassandraTable(implicit context: CassandraContext, session: CqlSession): String = {
    val tableName = generateTableName

    if (context.createScriptPath.nonEmpty)
      executeCreateQuery(script = context.createScriptPath, tableName = tableName)

    if (context.jsonDataPath.nonEmpty)
      insertJsonPayload(jsonPath = context.jsonDataPath, tableName = tableName)

    tableName
  }

  def generateTableName: String = {
    val cleanUuid = UUID.randomUUID().toString.replace("-", "")
    s"todo_items_$cleanUuid"
  }

  private def executeCreateQuery(script: String, tableName: String)(implicit session: CqlSession): Unit = {
    val source = scala.io.Source.fromFile(script)

    source.mkString
      .split(";")
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(_.replace("$tableName", tableName))
      .foreach(session.execute)

    source.close()
  }

  private def insertJsonPayload[T](jsonPath: String, tableName: String)(implicit session: CqlSession): Unit = {
    def addParsedObjects(collection: List[Json]): Unit = {
      collection.map { json =>
        val query = insertInto("whose_turn", tableName).json(json.noSpaces).build()
        session.execute(query)
      }
    }

    val source = scala.io.Source.fromFile(jsonPath)
    decode[List[Json]](source.mkString).map(addParsedObjects)

    source.close()
  }

  def entityTodoItem(tableName: String, entityId: UUID)(implicit session: CqlSession): List[TodoItemEntity] = {
    val result = for {
      query           <- IO(queryEntityWithIdBind(tableName))
      boundQuery      <- IO(session.prepare(query).bind(entityId))
      cassandraResult <- IO(session.execute(boundQuery).iterator().asScala)
      entities        <- toTodoItemEntity(cassandraResult)
    } yield entities

    result.unsafeRunSync()
  }

  override def afterAll(): Unit = {
    container.stop()
  }
}
