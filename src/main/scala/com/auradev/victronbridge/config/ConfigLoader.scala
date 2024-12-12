package com.auradev.victronbridge.config

import cats.effect.IO
import pureconfig._

object ConfigLoader:
  def loadConfig(): IO[AppConfig] = IO.delay:
    ConfigSource.default.loadOrThrow[AppConfig]
