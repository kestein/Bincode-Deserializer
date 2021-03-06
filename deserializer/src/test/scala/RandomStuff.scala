import com.kestein.deserializer.Deserializer.DeserializeResult
import com.kestein.deserializer.{Deserializer, FlatDeserializeConfig, MapDeserializeConfig}

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
 val one: Map[BigInt, RandomSubStuff],
 val two: Seq[String],
 val three: Byte,
 val four: Boolean,
 val five: Double,
 val six: Long,
 val seven: Option[Long],
 val eight: (Short, Short, Long, Long)
)

class RandomStuffFactory extends FlatDeserializeConfig[RandomStuff] {

  object OneMapConfig extends MapDeserializeConfig[BigInt, RandomSubStuff] {
    override def deserialize_key(d: Deserializer): DeserializeResult[BigInt] = {
      d.deserialize_u64()
    }

    override def deserialize_value(d: Deserializer): DeserializeResult[RandomSubStuff] = {
      d.deserialize_struct[RandomSubStuff](new RandomSubStuffFactory)
    }
  }

  object EightTupleConfig extends FlatDeserializeConfig[(Short, Short, Long, Long)] {
    override def deserialize(d: Deserializer): DeserializeResult[(Short, Short, Long, Long)] = {
      d.deserialize_u8().fold(err => Left(err), one => {
        d.deserialize_i16().fold(err => Left(err), two => {
          d.deserialize_u32().fold(err => Left(err), three => {
            d.deserialize_i64().fold(err => Left(err), four => {
              Right((one, two, three, four))
            })
          })
        })
      })
    }
  }

  override def deserialize(d: Deserializer): DeserializeResult[RandomStuff] = {
    d.deserialize_map[BigInt, RandomSubStuff](OneMapConfig).fold(err => Left(err), one => {
      d.deserialize_seq[String]((x: Deserializer) => x.deserialize_str()).fold(err => Left(err), two => {
        d.deserialize_i8().fold(err => Left(err), three => {
          d.deserialize_bool().fold(err => Left(err), four => {
            d.deserialize_f64().fold(err => Left(err), five => {
              d.deserialize_u32().fold(err => Left(err), six => {
                d.deserialize_option[Long]((x: Deserializer) => x.deserialize_i64()).fold(err => Left(err), seven => {
                  d.deserialize_tuple[(Short, Short, Long, Long)](EightTupleConfig).fold(err => Left(err), eight => {
                    Right(new RandomStuff(one, two, three, four, five, six, seven, eight))
                  })
                })
              })
            })
          })
        })
      })
    })
  }
}
