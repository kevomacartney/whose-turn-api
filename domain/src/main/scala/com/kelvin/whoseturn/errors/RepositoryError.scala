package com.kelvin.whoseturn.errors

trait RepositoryError

case object DatabaseError   extends RepositoryError
case object DatabaseOffline extends RepositoryError
case object QueryInvalid    extends RepositoryError
case object Failure         extends RepositoryError
