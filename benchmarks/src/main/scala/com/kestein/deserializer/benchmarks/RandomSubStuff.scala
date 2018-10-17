package com.kestein.deserializer.benchmarks

import com.kestein.deserializer.Deserializer.DeserializeResult
import com.kestein.deserializer.{Deserializer, FlatDeserializeConfig}

/*
pub struct com.kestein.deserializer.benchmarks.RandomSubStuff {
    one: usize,
    two: String,
    three: i64,
    four: Vec<Vec<u16>>,
    five: com.kestein.deserializer.benchmarks.MoreSubStuff
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
  override def deserialize(d: Deserializer): DeserializeResult[RandomSubStuff] = {
    d.deserialize_u64().fold(err => Left(err), one => {
      d.deserialize_str().fold(err => Left(err), two => {
        d.deserialize_i64().fold(err => Left(err), three => {
          d.deserialize_seq[Seq[Int]](
            (x: Deserializer) => x.deserialize_seq[Int](
              (xx: Deserializer) => xx.deserialize_u16()
            )
          ).fold(err => Left(err), four => {
            d.deserialize_enum(new MoreSubStuffFactory).fold(err => Left(err), five => {
              Right(new RandomSubStuff(one, two, three, four, five))
            })
          })
        })
      })
    })
  }
}
