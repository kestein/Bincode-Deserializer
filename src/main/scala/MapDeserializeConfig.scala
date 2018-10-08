trait MapDeserializeConfig[K, V] {
  def deserialize_key(d: Deserializer): K
  def deserialize_value(d: Deserializer): V
  def deserialize_entry(d: Deserializer): (K, V) = {
    (deserialize_key(d), deserialize_value(d))
  }
}
