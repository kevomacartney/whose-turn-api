package com.kelvin.whoseturn.errors

import com.kelvin.whoseturn.errors.meta.ErrorLocation

case class ValidationError(validatedFields: List[ValidatedField], errorLocation: ErrorLocation) extends Error
case class ValidatedField(field: String, message: String)
