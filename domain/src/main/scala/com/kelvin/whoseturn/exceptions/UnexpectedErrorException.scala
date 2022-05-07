package com.kelvin.whoseturn.exceptions

final case class UnexpectedErrorException(private val message: String, private val cause: Throwable = None.orNull)
    extends Exception(message, cause)
