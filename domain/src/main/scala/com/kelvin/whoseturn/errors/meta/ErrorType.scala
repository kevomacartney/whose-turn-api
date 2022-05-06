package com.kelvin.whoseturn.errors.meta

import com.kelvin.whoseturn.exceptions.EncodingException
import io.circe._

trait ErrorType
case object ValidationErrorType extends ErrorType

object ErrorType {
  implicit val encodeErrorType: Encoder[ErrorType] = {
    case ValidationErrorType => Json.fromString("ValidationError")
    case a: ErrorType        => throw EncodingException(s"Could not encode'${a.getClass.getName}' to ErrorType")
  }

  implicit val decodeErrorType: Decoder[ErrorType] = (c: HCursor) => {
    c.value.asString match {
      case Some("ValidationError") => Right[DecodingFailure, ErrorType](ValidationErrorType)
      case Some(value) =>
        Left[DecodingFailure, ErrorType](DecodingFailure(s"Could not decode $value to ErrorType", List()))
      case _ => Left[DecodingFailure, ErrorType](DecodingFailure(s"Could not decode string to ErrorType", List()))
    }
  }
}
