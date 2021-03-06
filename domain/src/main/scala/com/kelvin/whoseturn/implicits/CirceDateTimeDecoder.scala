package com.kelvin.whoseturn.implicits

import io.circe._
import org.joda.time.DateTime
import cats.implicits._

import scala.util.Try

object CirceDateTimeDecoder {
  implicit val decodeDateTime: Decoder[DateTime] = Decoder.decodeString.emap { str =>
    Try(DateTime.parse(str)).toEither
      .leftMap(_.getLocalizedMessage)
  }
}
