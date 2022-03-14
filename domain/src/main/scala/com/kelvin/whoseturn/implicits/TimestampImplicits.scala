package com.kelvin.whoseturn.implicits
import org.joda.time.DateTime

import java.sql.Timestamp
import java.time.Instant

object TimestampImplicits {
  implicit class TimestampConversion(datetime: DateTime) {
    def toTimestamp: Timestamp = {
      val javaInstant = Instant.ofEpochMilli(datetime.toInstant.getMillis)
      Timestamp.from(javaInstant)
    }
  }
}
