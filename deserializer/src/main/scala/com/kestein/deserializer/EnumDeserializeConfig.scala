package com.kestein.deserializer

import com.kestein.deserializer.Deserializer.DeserializeResult

trait EnumDeserializeConfig[T] {
  def deserialize(variant: Long, d: Deserializer): DeserializeResult[T]
}
