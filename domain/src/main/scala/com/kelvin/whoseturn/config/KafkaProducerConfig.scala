package com.kelvin.whoseturn.config

case class KafkaProducerConfig(
    schemaServers: String,
    schemaName: String,
    topicName: String,
    bootstrapServers: String,
    clientId: String,
    acknowledge: Int,
    retries: Int,
    batchSize: Int
)
