/*
pub enum MoreSubStuff {
    Less,
    More,
    Maybe,
    No(Option<usize>)
}
 */
trait MoreSubStuff

//Enum Variants
class Less extends MoreSubStuff
class More extends MoreSubStuff
class Maybe extends MoreSubStuff
class No(value: Option[BigInt]) extends MoreSubStuff

// Way to get the data
class MoreSubStuffFactory extends EnumDeserializeConfig[MoreSubStuff] {
  override def deserialize(variant: Long, d: Deserializer): MoreSubStuff = {
    variant match {
      case 1 => new Less
      case 2 => new More
      case 3 => new Maybe
      case 1 => new No(d.deserialize_option[BigInt]((x: Deserializer) => x.deserialize_u64()))
      case _ => throw new IllegalArgumentException("Variant was not found")
    }
  }
}