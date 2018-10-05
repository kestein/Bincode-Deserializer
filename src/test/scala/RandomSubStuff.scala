/*
pub struct RandomSubStuff {
    one: usize,
    two: String,
    three: i64,
    four: Vec<Vec<u16>>,
    five: MoreSubStuff
}
 */

class RandomSubStuff(
  val one: BigInt,
  val two: String,
  val three: Long,
  val four: Seq[Seq[Int]],
  val five: MoreSubStuff
)

class RandomSubStuffFactory extends FlatDeserializeConfig[RandomSubStuff] {
  override def deserialize(d: Deserializer): RandomSubStuff = {
    val one = d.deserialize_u64()
    val two = d.deserialize_str()
    val three = d.deserialize_i64()
    val four = d.deserialize_seq[Seq[Int]](
      (x: Deserializer) => x.deserialize_seq[Int](
        (xx: Deserializer) => xx.deserialize_u16()
      )
    )
    val five = d.deserialize_enum(new MoreSubStuffFactory)
    new RandomSubStuff(one, two, three, four, five)
  }
}
