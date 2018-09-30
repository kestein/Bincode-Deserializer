trait FlatDeserializeConfig[T] {
  // How to deserialize the return type
  def deserialize(d: Deserializer): T
}
