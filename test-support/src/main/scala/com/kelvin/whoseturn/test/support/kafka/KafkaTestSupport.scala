package com.kelvin.whoseturn.test.support.kafka

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.GenericContainer
import fs2.kafka._
import io.circe.syntax._
import org.apache.kafka.clients.admin._
import org.apache.kafka.clients.consumer.ConsumerConfig._
import org.testcontainers.containers._
import org.testcontainers.utility.DockerImageName
import sttp.client3._
import sttp.client3.quick.backend

import java.util.UUID
import scala.jdk.CollectionConverters._
import scala.util.Random

case class KafkaTestSupportConfig(bootstrapServer: String, schemaServer: String, randomTopicName: String)

trait KafkaTestSupport {
  import KafkaTestSupport._

  protected var clusterInitialised = false;

  private val kafkaImage: DockerImageName               = DockerImageName.parse("confluentinc/cp-kafka:6.1.1")
  private val brokerId: String                          = Random.nextInt(69).toString
  private val kafkaHostName: String                     = s"kafka-$brokerId"
  private val kafkaNetwork: Network                     = Network.newNetwork()
  protected implicit val kafkaContainer: KafkaContainer = new KafkaContainer(kafkaImage)

  private val schemaServerImage: DockerImageName                      = DockerImageName.parse("confluentinc/cp-schema-registry:6.1.1")
  protected implicit val schemaServerContainer: SchemaServerContainer = new GenericContainer(schemaServerImage.toString)

  private def initKafka(): Unit = {
    val envVars = Map[String, String](
      "KAFKA_BROKER_ID"                 -> brokerId,
      "KAFKA_HOST_NAME"                 -> kafkaHostName,
      "KAFKA_AUTO_CREATE_TOPICS_ENABLE" -> "false"
    ).asJava

    kafkaContainer
      .withNetwork(kafkaNetwork)
      .withNetworkAliases(kafkaHostName)
      .withEnv(envVars)
      .start()

  }

  private def initSchemaRegistry(): Unit = {
    val envVars = Map[String, String](
      "SCHEMA_REGISTRY_HOST_NAME"                    -> schemaServerContainer.container.getHost,
      "SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS" -> s"$kafkaHostName:9092"
    ).asJava

    schemaServerContainer.container
      .withExposedPorts(schemaServerPort)

    schemaServerContainer.container
      .withEnv(envVars)

    schemaServerContainer.container
      .withNetwork(kafkaNetwork)

    schemaServerContainer.start()
  }

  private def startEnsemble(): Unit = {
    if (clusterInitialised) return

    initKafka()
    initSchemaRegistry()

    clusterInitialised = true
  }

  private def kafkaHelperConfig(topicName: String): KafkaTestSupportConfig = {
    KafkaTestSupportConfig(
      bootstrapServer = kafkaContainer.getBootstrapServers,
      schemaServer = schemaServerUrl,
      randomTopicName = topicName
    )
  }

  def withRunningKafkaCluster[T](f: KafkaTestSupportConfig => T): T = {
    startEnsemble()

    withRandomKafkaTopic { randomTopicNme =>
      val helperConfig = kafkaHelperConfig(randomTopicNme)
      f(helperConfig)
    }
  }

  private def withRandomKafkaTopic[T](f: String => T): T = {
    val randomTopicName = UUID.randomUUID().toString
    createKafkaTopic(randomTopicName)

    f(randomTopicName)
  }
}

object KafkaTestSupport {
  type SchemaServerContainer = GenericContainer
  private val schemaServerPort = 8081

  private def schemaServerUrl(implicit schemaServerContainer: SchemaServerContainer): String = {
    s"http://${schemaServerContainer.container.getHost}:${schemaServerContainer.container.getMappedPort(schemaServerPort)}"
  }

  def createKafkaTopic(topicName: String)(implicit kafkaContainer: KafkaContainer): Unit = {
    withKafkaAdminClient { adminClient =>
      val topicDefinition = new NewTopic(topicName, 1, 1.toShort)
      adminClient.createTopics(List(topicDefinition).asJava)
    }
  }

  def withKafkaAdminClient[T](f: AdminClient => T)(implicit kafkaContainer: KafkaContainer): T = {
    val config      = Map[String, AnyRef](BOOTSTRAP_SERVERS_CONFIG -> kafkaContainer.getBootstrapServers).asJava
    val adminClient = AdminClient.create(config)

    f(adminClient)
  }

  def registerSchemaToKafkaSchemaRegistry(schemaName: String, schemaPath: String)(
      implicit schemaContainer: SchemaServerContainer
  ): Unit = {
    val request = defineCreateSchemaRequest(schemaName, schemaPath)

    request.send(backend).body match {
      case Left(error) => throw new Exception(s"Could add schema due to error [error=$error]")
      case _           => ()
    }
  }

  private def defineCreateSchemaRequest(schemaName: String, schemaPath: String)(
      implicit schemaContainer: SchemaServerContainer
  ) = {
    val uri  = uri"$schemaServerUrl/subjects/$schemaName/versions"
    val body = defineCreateSchemaRequestBody(schemaPath)

    basicRequest
      .post(uri)
      .header("Content-Type", "application/vnd.schemaregistry.v1+json")
      .body(body)
  }

  private def defineCreateSchemaRequestBody(schemaPath: String): String = {
    val schema = scala.io.Source.fromFile(schemaPath).mkString
    Map("schema" -> schema).asJson.noSpaces
  }

  def consumeLastMessageFromTopic[KEY, VALUE](
      keyDeserializer: fs2.kafka.Deserializer[IO, KEY],
      valueDeserializer: fs2.kafka.RecordDeserializer[IO, VALUE]
  )(
      implicit kafkaTestSupportConfig: KafkaTestSupportConfig
  ): Option[VALUE] = {
    withConsumer(keyDeserializer, valueDeserializer) { consumer =>
      consumer.stream
        .map(_.record.value)
        .take(1)
        .compile
        .last
        .unsafeRunSync()
    }
  }

  def withConsumer[KEY, VALUE, T](
      keyDeserializer: fs2.kafka.Deserializer[IO, KEY],
      valueDeserializer: fs2.kafka.RecordDeserializer[IO, VALUE]
  )(
      f: fs2.kafka.KafkaConsumer[IO, KEY, VALUE] => Option[T]
  )(implicit kafkaConfig: KafkaTestSupportConfig): Option[T] = {

    val consumerSettings = ConsumerSettings[IO, KEY, VALUE](keyDeserializer, valueDeserializer)
      .withBootstrapServers(kafkaConfig.bootstrapServer)
      .withClientId(UUID.randomUUID().toString)
      .withGroupId(UUID.randomUUID().toString)
      .withAutoOffsetReset(AutoOffsetReset.Earliest)

    fs2.kafka.KafkaConsumer
      .stream(consumerSettings)
      .subscribeTo(kafkaConfig.randomTopicName)
      .through(_.map(consumer => f(consumer)))
      .compile
      .last
      .unsafeRunSync()
      .flatten
  }
}
