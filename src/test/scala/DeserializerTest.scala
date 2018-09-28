import java.io.{ByteArrayInputStream, InputStream}
import java.nio.{ByteBuffer, ByteOrder}

import org.scalatest.FunSuite

class DeserializerTest extends FunSuite {
  test("Deserializer.deserialize_u8") {
    val expected = 97
    val d = makeDeserializer(1, (x:ByteBuffer) => x.put(expected.toByte))
    assert(d.deserialize_u8() == expected.toShort)
  }
  test("Deserializer.deserialize_i8") {
    val expected = -97
    val d = makeDeserializer(1, (x:ByteBuffer) => x.put(expected.toByte))
    assert(d.deserialize_i8() == expected.toByte)
  }
  test("Deserializer.deserialize_u16") {
    val expected = 300
    val d = makeDeserializer(2, (x:ByteBuffer) => x.putShort(expected.toShort))
    assert(d.deserialize_u16() == expected.toInt)
  }
  test("Deserializer.deserialize_i16") {
    val expected = -300
    val d = makeDeserializer(2, (x:ByteBuffer) => x.putShort(expected.toShort))
    assert(d.deserialize_i16() == expected.toShort)
  }
  test("Deserializer.deserialize_u32") {
    val expected = 300000
    val d = makeDeserializer(4, (x:ByteBuffer) => x.putInt(expected))
    assert(d.deserialize_u32() == expected.toLong)
  }
  test("Deserializer.deserialize_i32") {
    val expected = -300000
    val d = makeDeserializer(4, (x:ByteBuffer) => x.putInt(expected))
    assert(d.deserialize_i32() == expected)
  }
  test("Deserializer.deserialize_u64") {
    val expected: Long = Long.MaxValue
    val d = makeDeserializer(8, (x:ByteBuffer) => x.putLong(expected))
    assert(d.deserialize_u64().toLong == expected)
  }
  test("Deserializer.deserialize_i64") {
    val expected = Long.MinValue
    val d = makeDeserializer(8, (x:ByteBuffer) => x.putLong(expected))
    assert(d.deserialize_i64() == expected)
  }
  test("Deserializer.deserialize_f32") {
    val expected = Float.MinValue
    val d = makeDeserializer(8, (x:ByteBuffer) => x.putFloat(expected))
    assert(d.deserialize_f32() == expected)
  }
  test("Deserializer.deserialize_f64") {
    val expected = Double.MinValue
    val d = makeDeserializer(8, (x:ByteBuffer) => x.putDouble(expected))
    assert(d.deserialize_f64() == expected)
  }

  def makeDeserializer(capacity: Int, insertVal: ByteBuffer=>ByteBuffer): Deserializer = {
    val byteRep = ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN)
    new Deserializer(new ByteArrayInputStream(insertVal(byteRep).array()))
  }
}
