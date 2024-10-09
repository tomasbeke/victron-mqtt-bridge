package com.auradev.victronbridge.model

import java.time.Instant

trait Event {
  def key: String
  def timestamp: Instant
}
