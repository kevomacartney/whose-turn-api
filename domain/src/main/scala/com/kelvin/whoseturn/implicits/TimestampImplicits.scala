package com.kelvin.whoseturn.implicits
import io.circe.{Decoder, Encoder}
import org.joda.time.DateTime
import cats.implicits._

import java.sql.Timestamp
import java.time.Instant
import scala.util.Try

object TimestampImplicits {
  implicit class TimestampConversion(datetime: DateTime) {
    def toTimestamp: Timestamp = {
      val javaInstant = Instant.ofEpochMilli(datetime.toInstant.getMillis)
      Timestamp.from(javaInstant)
    }
  }

  implicit val encodeTimestamp: Encoder[Timestamp] = Encoder.encodeString.contramap(_.toString)
  implicit val decoderTimestamp: Decoder[Timestamp] = Decoder.decodeString.emap { str =>
    Try(Timestamp.valueOf(str)).toEither.leftMap(_.getLocalizedMessage)
  }
}
