package com.kelvin.whoseturn.repositories
import com.kelvin.whoseturn.errors.Error

import cats.data.EitherT

trait GenericRepository[F[_], T] {
  def add(item: T): F[Either[Error, T]]
}
