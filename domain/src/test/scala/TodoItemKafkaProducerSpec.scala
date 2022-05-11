import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.codahale.metrics.MetricRegistry
import com.kelvin.whoseturn.config.KafkaProducerConfig
import com.kelvin.whoseturn.errors.kafka.ProducerError
import com.kelvin.whoseturn.fixtures.TodoItemUserActionEventFixture
import com.kelvin.whoseturn.kafka.messages.Codecs.WhoseTurnTodoItemEventCodec._
import com.kelvin.whoseturn.kafka.messages.TodoItemUserActionEvent
import com.kelvin.whoseturn.kafka.producer.TodoItemKafkaProducer
import com.kelvin.whoseturn.support.kafka.KafkaTestSupport._
import com.kelvin.whoseturn.support.kafka.{KafkaTestSupport, KafkaTestSupportConfig}
import fs2.kafka._
import fs2.kafka.vulcan._
import org.scalamock.scalatest.MockFactory
import org.scalatest.EitherValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.testcontainers.containers.KafkaContainer

import java.util.UUID

class TodoItemKafkaProducerSpec
    extends AnyWordSpec
    with Matchers
    with KafkaTestSupport
    with TodoItemUserActionEventFixture
    with EitherValues {
  import TodoItemKafkaProducerSpec._

  "TodoItemKafkaProducer" should {
    "successfully produces messages to kafka cluster" in {
      withRunningKafkaCluster { implicit config =>
        prepareCluster()

        withTodoItemKafkaProducer { producer =>
          val messageToProduce = WhoseTurnTodoItemEventFixture()

          producer.add(messageToProduce).unsafeRunSync()
          val producedMessage = consumeLastMessageFromTopic(uuidDeserializer, whoseTurnTodoItemEventDeserializer).get

          messageToProduce mustBe producedMessage
        }
      }
    }

    "return an error" when {
      "unable to write to produce" in {
        withFailingMockedProducer { producer =>
          val messageToProduce = WhoseTurnTodoItemEventFixture()

          val result = producer.add(messageToProduce).unsafeRunSync().left.value

          result must matchPattern { case ProducerError("There was an error while producing event to kafka", _) => }
        }
      }
    }
  }
}

object TodoItemKafkaProducerSpec extends MockFactory {
  lazy val WhoseTurnTodoItemEventSchemaName = "TodoItemUserActionEvent"

  def whoseTurnTodoItemEventDeserializer(
      implicit kafkaConfig: KafkaTestSupportConfig
  ): RecordDeserializer[IO, TodoItemUserActionEvent] = {
    val avroSettings = AvroSettings(SchemaRegistryClientSettings[IO](kafkaConfig.schemaServer))
      .withAutoRegisterSchemas(true)

    avroDeserializer.using(avroSettings)
  }

  val uuidDeserializer: Deserializer[IO, UUID] = Deserializer[IO, UUID]

  def prepareCluster()(
      implicit kafkaHelperConfig: KafkaTestSupportConfig,
      schemaServerContainer: SchemaServerContainer,
      kafkaContainer: KafkaContainer
  ): Unit = {
    registerWhoseTurnEventSchema
    createKafkaTopic(kafkaHelperConfig.randomTopicName)
  }

  def registerWhoseTurnEventSchema(
      implicit config: KafkaTestSupportConfig,
      schemaServerContainer: SchemaServerContainer
  ): Unit = {
    val schemaPath = getClass.getResource("/schemas/whose_turn_event.avsc").getPath
    registerSchemaToKafkaSchemaRegistry(schemaName = WhoseTurnTodoItemEventSchemaName, schemaPath)
  }

  def withTodoItemKafkaProducer[T](
      f: TodoItemKafkaProducer => T
  )(implicit kafkaTestSupportConfig: KafkaTestSupportConfig): T = {
    val kafkaConfig: KafkaProducerConfig = KafkaProducerConfig(
      schemaServers = kafkaTestSupportConfig.schemaServer,
      topicName = kafkaTestSupportConfig.randomTopicName,
      bootstrapServers = kafkaTestSupportConfig.bootstrapServer,
      schemaName = WhoseTurnTodoItemEventSchemaName,
      clientId = UUID.randomUUID().toString,
      acknowledge = 1,
      retries = 1,
      batchSize = 1
    )

    implicit val metricsRegistry: MetricRegistry = new MetricRegistry()
    TodoItemKafkaProducer
      .resource(kafkaConfig)
      .use { producer =>
        IO(f(producer))
      }
      .unsafeRunSync()
  }

  def withFailingMockedProducer[T](f: TodoItemKafkaProducer => T): T = {
    val mockedProducer = mock[KafkaProducer[IO, UUID, TodoItemUserActionEvent]]

    (mockedProducer.produce _)
      .expects(*)
      .returning(IO(IO.raiseError(new Exception("KABOOOOM!"))))

    implicit val metricsRegistry: MetricRegistry = new MetricRegistry()
    val todoItemProduce = new TodoItemKafkaProducer(mockedProducer, "")
    f(todoItemProduce)
  }
}
