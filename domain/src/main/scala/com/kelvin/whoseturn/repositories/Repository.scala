package com.kelvin.whoseturn.repositories

import java.util.UUID
import scala.language.higherKinds

trait Repository[F[_]] {
  def get(id: String): F[Option[RepositoryItem]]
}

final case class RepositoryItem(name: String, id: UUID)