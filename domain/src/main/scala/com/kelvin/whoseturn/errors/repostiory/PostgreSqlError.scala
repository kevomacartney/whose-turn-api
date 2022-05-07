package com.kelvin.whoseturn.errors.repostiory
import com.kelvin.whoseturn.errors.{Error, RepositoryError}

case class PostgreSqlError(cause: RepositoryError, message: String) extends Error
