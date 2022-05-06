package com.kelvin.whoseturn.kafka.producer

import cats.MonadError
import cats.data.EitherT
import cats.effect._
import cats.syntax.all._
import com.kelvin.whoseturn.config.KafkaProducerConfig
import com.kelvin.whoseturn.errors.kafka.ProducerError
import com.kelvin.whoseturn.kafka._
import com.kelvin.whoseturn.kafka.messages._
import com.kelvin.whoseturn.repositories.GenericRepository
import vulcan._
import fs2.kafka._
import fs2.kafka.vulcan._
import org.apache.kafka.clients.producer.ProducerConfig._
import com.kelvin.whoseturn.kafka.messages.Codecs.WhoseTurnTodoItemEventCodec._
import com.kelvin.whoseturn.kafka.producer.TodoItemKafkaProducer.createProducerSettings
import com.typesafe.scalalogging.LazyLogging
import org.slf4j.MDC

import java.util.UUID

class TodoItemKafkaProducer(producer: KafkaProducer[IO, UUID, TodoItemUserActionEvent], topicName: String)
    extends GenericRepository[IO, TodoItemUserActionEvent]
    with LazyLogging {
  override def add(
      action: TodoItemUserActionEvent
  ): IO[Either[ProducerError, TodoItemUserActionEvent]] = {
    for {
      record <- IO(ProducerRecord(topicName, action.todoId, action))
      _      <- IO(logger.info(s"Publishing todo action [todo.id=${action.todoId}, action=${action.action}]"))
      output <- producer
                 .produce(ProducerRecords.one(record))
                 .flatten
                 .map(_ => action.asRight[ProducerError])
                 .handleError(handleProduceError)
    } yield output
  }

  def handleProduceError(throwable: Throwable): Either[ProducerError, TodoItemUserActionEvent] = {
    logger.error("Could not produce todo action due to error", throwable)

    ProducerError(message = "There was an error while producing event to kafka", cause = Some(throwable)).asLeft
  }
}

object TodoItemKafkaProducer {
  def resource(kafkaProducerConfig: KafkaProducerConfig): Resource[IO, TodoItemKafkaProducer] = {

    val keySerializerResource   = Resource.eval(IO(Serializer[IO, UUID]))
    val valueSerializerResource = avroSerializerSettings(kafkaProducerConfig)

    for {
      keySerializer    <- keySerializerResource
      valueSerializer  <- valueSerializerResource
      producerSettings = createProducerSettings(kafkaProducerConfig, keySerializer, valueSerializer)

      todoItemKafkaProducer <- KafkaProducer
                                .resource(producerSettings)
                                .map(new TodoItemKafkaProducer(_, kafkaProducerConfig.topicName))
    } yield todoItemKafkaProducer
  }

  private def avroSerializerSettings(
      kafkaConfig: KafkaProducerConfig
  ): Resource[IO, RecordSerializer[IO, TodoItemUserActionEvent]] = {
    val avroSettings = AvroSettings(SchemaRegistryClientSettings[IO](kafkaConfig.schemaServers))
      .withAutoRegisterSchemas(true)

    val acquire = IO(avroSerializer.using(avroSettings))

    Resource.eval(acquire)
  }

  private def createProducerSettings(
      kafkaProducerConfig: KafkaProducerConfig,
      keySerializer: Serializer[IO, UUID],
      valueSerializer: RecordSerializer[IO, TodoItemUserActionEvent]
  ): ProducerSettings[IO, UUID, TodoItemUserActionEvent] = {
    ProducerSettings(keySerializer = keySerializer, valueSerializer = valueSerializer)
      .withBootstrapServers(kafkaProducerConfig.bootstrapServers)
      .withAcks(Acks.One)
      .withRetries(kafkaProducerConfig.retries)
      .withBatchSize(kafkaProducerConfig.batchSize)
      .withClientId(kafkaProducerConfig.clientId)
      .withProperties(METRIC_REPORTER_CLASSES_CONFIG -> classOf[KafkaMetricsReporter].getName)
  }
}
