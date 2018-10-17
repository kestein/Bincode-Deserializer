package com.kestein.deserializer.benchmarks

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer

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
