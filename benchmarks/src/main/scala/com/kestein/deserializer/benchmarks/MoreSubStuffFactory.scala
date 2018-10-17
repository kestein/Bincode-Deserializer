package com.kestein.deserializer.benchmarks

import com.kestein.deserializer.Deserializer.DeserializeResult
import com.kestein.deserializer.{Deserializer, EnumDeserializeConfig}

// Way to get the data
class MoreSubStuffFactory extends EnumDeserializeConfig[MoreSubStuff] {
  override def deserialize(variant: Long, d: Deserializer): DeserializeResult[MoreSubStuff] = {
    variant match {
      case 0 => Right(new Less)
      case 1 => Right(new More)
      case 2 => Right(new Maybe)
      case 3 =>
        d.deserialize_option[BigInt](x => x.deserialize_u64()).fold(err => Left(err), maybe_num => {
          Right(new No(maybe_num))
        })
      case i@_ =>
        var err = new StringBuilder("The Variant was not found ")
        err = err.append(i.toString)
        throw new IllegalArgumentException(err.toString())
    }
  }
}
