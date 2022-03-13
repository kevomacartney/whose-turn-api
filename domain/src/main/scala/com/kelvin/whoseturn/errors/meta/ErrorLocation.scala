package com.kelvin.whoseturn.errors.meta

import com.kelvin.whoseturn.exceptions.EncodingException
import io.circe._

trait ErrorLocation
case object BodyError extends ErrorLocation

object ErrorLocation {
  implicit val encodeErrorLocation: Encoder[ErrorLocation] = {
    case BodyError        => Json.fromString("Body")
    case a: ErrorLocation => throw EncodingException(s"Could not decode'${a.getClass.getName}' to ErrorLocation")
  }

  implicit val decodeErrorLocation: Decoder[ErrorLocation] = (c: HCursor) => {
    c.value.asString match {
      case Some("Body") => Right[DecodingFailure, ErrorLocation](BodyError)
      case Some(value) =>
        Left[DecodingFailure, ErrorLocation](DecodingFailure(s"Could not decode '$value' to ErrorLocation", List()))
      case _ => Left[DecodingFailure, ErrorLocation](DecodingFailure(s"Could not decode string to ErrorType", List()))
    }
  }
}
