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
class No(val value: Option[BigInt]) extends MoreSubStuff

// Way to get the data
class MoreSubStuffFactory extends EnumDeserializeConfig[MoreSubStuff] {
  override def deserialize(variant: Long, d: Deserializer): MoreSubStuff = {
    variant match {
      case 0 => new Less
      case 1 => new More
      case 2 => new Maybe
      case 3 => new No(d.deserialize_option[BigInt]((x: Deserializer) => x.deserialize_u64()))
      case i@_ =>
        var err = new StringBuilder("The Variant was not found ")
        err = err.append(i.toString)
        throw new IllegalArgumentException(err.toString())
    }
  }
}
