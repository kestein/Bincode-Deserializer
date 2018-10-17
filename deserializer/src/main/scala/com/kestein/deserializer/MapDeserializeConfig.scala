package com.kestein.deserializer

import com.kestein.deserializer.Deserializer.DeserializeResult

trait MapDeserializeConfig[K, V] {
  def deserialize_key(d: Deserializer): DeserializeResult[K]
  def deserialize_value(d: Deserializer): DeserializeResult[V]
  def deserialize_entry(d: Deserializer): DeserializeResult[(K, V)] = {
    deserialize_key(d).fold[DeserializeResult[(K, V)]](err => Left(err), key => {
      deserialize_value(d).fold[DeserializeResult[(K, V)]](err => Left(err), value => {
        Right((key, value))
      })
    })
  }
}
