package com.kelvin.whoseturn.config

final case class PostgresqlConfig(host: String, port: Int, table: String, username: String, password: String)