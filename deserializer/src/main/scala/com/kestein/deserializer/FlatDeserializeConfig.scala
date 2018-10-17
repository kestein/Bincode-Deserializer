package com.kestein.deserializer

import com.kestein.deserializer.Deserializer.DeserializeResult

trait FlatDeserializeConfig[T] {
  // How to deserialize the return type
  def deserialize(d: Deserializer): DeserializeResult[T]
}
