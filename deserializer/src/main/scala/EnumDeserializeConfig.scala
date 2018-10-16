import Deserializer.DeserializeResult

trait EnumDeserializeConfig[T] {
  def deserialize(variant: Long, d: Deserializer): DeserializeResult[T]
}
