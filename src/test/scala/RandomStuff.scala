/*
pub struct RandomStuff {
  one: HashMap<usize, RandomSubStuff>,
  two: Vec<String>,
  three: i8,
  four: bool,
  five: f64,
  six: u32,
  seven: Option<i64>,
  eight: (u8, i16, u32, i64),
}
*/

class RandomStuff(
 one: Map[BigInt, RandomSubStuff],
 two: Seq[String],
 three: Byte,
 four: Boolean,
 five: Double,
 six: Long,
 seven: Option[Long],
 eight: (Short, Short, Long, Long)
)

class RandomStuffFactory extends FlatDeserializeConfig[RandomStuff] {

  object OneMapConfig extends MapDeserializeConfig[BigInt, RandomSubStuff] {
    override def deserialize_key(d: Deserializer): BigInt = {
      d.deserialize_u64()
    }

    override def deserialize_value(d: Deserializer): RandomSubStuff = {
      d.deserialize_struct[RandomSubStuff](new RandomSubStuffFactory)
    }
  }

  object EightTupleConfig extends FlatDeserializeConfig[(Short, Short, Long, Long)] {
    override def deserialize(d: Deserializer): (Short, Short, Long, Long) = {
      (d.deserialize_u8(), d.deserialize_i16(), d.deserialize_u32(), d.deserialize_i64())
    }
  }

  override def deserialize(d: Deserializer): RandomStuff = {
    val one = d.deserialize_map[BigInt, RandomSubStuff](OneMapConfig)
    val two = d.deserialize_seq[String]((x: Deserializer) => x.deserialize_str())
    val three = d.deserialize_i8()
    val four = d.deserialize_bool()
    val five = d.deserialize_f64()
    val six = d.deserialize_u32()
    val seven = d.deserialize_option[Long]((x: Deserializer) => x.deserialize_i64())
    val eight = d.deserialize_tuple[(Short, Short, Long, Long)](EightTupleConfig)
    new RandomStuff(one, two, three, four, five, six, seven, eight)
  }
}