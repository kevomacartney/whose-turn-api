package com.kelvin.whoseturn.errors.kafka
import com.kelvin.whoseturn.errors.Error

final case class ProducerError(message: String, cause: Option[Throwable] = None) extends Error
