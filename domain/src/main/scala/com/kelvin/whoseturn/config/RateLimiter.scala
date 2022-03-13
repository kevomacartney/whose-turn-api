package com.kelvin.whoseturn.config

import scala.concurrent.duration.Duration

case class RateLimiter(rate: Int, duration: Duration)
