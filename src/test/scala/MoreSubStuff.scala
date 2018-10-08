import Deserializer.DeserializeResult
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.annotation.JsonSerialize
/*
pub enum MoreSubStuff {
    Less,
    More,
    Maybe,
    No(Option<usize>)
}
 */
@JsonSerialize(using = classOf[MoreSubStuffSerializer])
trait MoreSubStuff

//Enum Variants
class Less extends MoreSubStuff
class More extends MoreSubStuff
class Maybe extends MoreSubStuff
class No(val no: Option[BigInt]) extends MoreSubStuff

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

class MoreSubStuffSerializer extends StdSerializer[MoreSubStuff](classOf[MoreSubStuff]) {
  override def serialize(value: MoreSubStuff, gen: JsonGenerator, provider: SerializerProvider): Unit = {
    value match {
      case _: Less => gen.writeString("Less")
      case _: More => gen.writeString("More")
      case _: Maybe => gen.writeString("Maybe")
      case x: No =>
        gen.writeStartObject()
        x.no match {
          case Some(num) =>
            gen.writeFieldName("No")
            gen.writeNumber(num.underlying())
          case None => gen.writeNullField("No")
        }
        gen.writeEndObject()
    }
  }
}
