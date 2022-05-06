package com.kelvin.whoseturn.config

import com.typesafe.config.Config

object ConfigOverrides {
  implicit class PureConfigOverride(config: Config) {

    /**
      * Will override the config values of the given with the values of the parameter
      * @param toOverride The config to override with
      * @return
      */
    def withOverride(toOverride: Config): Config = {
      toOverride.withFallback(config)
    }
  }
}
