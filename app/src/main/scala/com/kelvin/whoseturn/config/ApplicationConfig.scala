package com.kelvin.whoseturn.config

final case class ApplicationConfig(
    opsServerConfig: OpsServerConfig,
    restConfig: RestApiConfig,
    cassandraConfig: CassandraConfig,
    postgresqlConfig: PostgresqlConfig
)

final case class RestApiConfig(port: Int, apiVersion: String)
final case class OpsServerConfig(port: Int)
final case class CassandraConfig(contactPoint: String, port: Int)
