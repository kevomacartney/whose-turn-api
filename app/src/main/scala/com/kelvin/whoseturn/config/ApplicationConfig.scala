package com.kelvin.whoseturn.config

final case class ApplicationConfig(
    opsServerConfig: OpsServerConfig,
    restConfig: RestApiConfig,
    postgresqlConfig: PostgresqlConfig
)

final case class RestApiConfig(port: Int, apiVersion: String)
final case class OpsServerConfig(port: Int)
