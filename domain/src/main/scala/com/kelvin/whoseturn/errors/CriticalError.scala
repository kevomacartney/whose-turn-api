package com.kelvin.whoseturn.errors

case class CriticalError(message: String) extends Error

object CriticalError {
  def defaultError: CriticalError = {
    CriticalError(message = "There was an internal server error, please try again shortly.")
  }
}
