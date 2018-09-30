trait MapDeserializeConfig[K, V] {
  def deserialize_key(d: Deserializer): K
  def deserialize_value(d: Deserializer): V
}
