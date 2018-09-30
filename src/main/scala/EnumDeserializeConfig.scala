trait EnumDeserializeConfig[T] {
  def deserialize(variant: Long, d: Deserializer): T
}
