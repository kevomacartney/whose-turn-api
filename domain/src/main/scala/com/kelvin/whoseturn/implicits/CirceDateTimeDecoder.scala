package com.kelvin.whoseturn.implicits

import io.circe._
import org.joda.time.DateTime
import cats.implicits._

import java.sql.Timestamp
import scala.util.Try

object CirceDateTimeDecoder {
  implicit val decodeDateTime: Decoder[DateTime] = Decoder.decodeString.emap { str =>
    Try(DateTime.parse(str)).toEither
      .leftMap(_.getLocalizedMessage)
  }
}

object CirceTimestampEncoder {
  implicit val encodeTimestamp: Encoder[Timestamp] = Encoder.encodeString.contramap(_.toString)
  implicit val decoderTimestamp: Decoder[Timestamp] = Decoder.decodeString.emap { str =>
    Try(Timestamp.valueOf(str)).toEither.leftMap(_.getLocalizedMessage)
  }
}
