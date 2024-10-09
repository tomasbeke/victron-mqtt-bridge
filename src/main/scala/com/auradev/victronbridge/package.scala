package com.auradev

package object victronbridge {
  type Topic = String

  object MaskedMap {
    def apply[V](map: Map[String, V]): MaskedMap[V] = new MaskedMap(map)
  }

  class MaskedMap[V](originalMap: Map[String, V]) {
    private val maskedMap: Map[String, V] = originalMap.map { case (k, v) => (k.replaceAll("\\d+", "+"), v) }

    def getMasked(key: String): Option[V] =
      maskedMap.get(key.replaceAll("\\d+", "+"))
  }
}
