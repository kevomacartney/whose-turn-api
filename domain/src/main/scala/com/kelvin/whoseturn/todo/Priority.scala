package com.kelvin.whoseturn.todo
import io.circe._

trait Priority

case object HighPriority     extends Priority
case object MediumPriority   extends Priority
case object LowPriority      extends Priority
case object ImminentPriority extends Priority

object Priority {
  implicit val encodePriority: Encoder[Priority] = {
    case priority: Priority => Json.fromString(priority.toString)
  }

  implicit val decodeErrorLocation: Decoder[Priority] = (c: HCursor) => {
    c.value.asString match {
      case Some("HighPriority")     => Right[DecodingFailure, Priority](HighPriority)
      case Some("MediumPriority")   => Right[DecodingFailure, Priority](MediumPriority)
      case Some("LowPriority")      => Right[DecodingFailure, Priority](LowPriority)
      case Some("ImminentPriority") => Right[DecodingFailure, Priority](ImminentPriority)

      case Some(value) =>
        Left[DecodingFailure, Priority](DecodingFailure(s"Could not decode '$value' to Priority", List()))
      case _ => Left[DecodingFailure, Priority](DecodingFailure(s"Could not decode string to Priority", List()))
    }
  }

  override def toString: String = {
    this.getClass.getSimpleName.replace("$", "")
  }
}
