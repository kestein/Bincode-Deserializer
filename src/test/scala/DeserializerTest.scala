import java.io.{ByteArrayInputStream, InputStream}
import java.nio.{ByteBuffer, ByteOrder}

import org.scalatest.FunSuite

class DeserializerTest extends FunSuite {
  test("Deserializer.deserialize_bool") {
    // False
    var d = makeDeserializer(1, (x:ByteBuffer) => x.put(0.toByte))
    assert(!d.deserialize_bool())
    // True
    d = makeDeserializer(1, (x:ByteBuffer) => x.put(1.toByte))
    assert(d.deserialize_bool())
  }
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
  test("Deserializer.deserialize_multiple_numbers") {
    val expectedOne = Double.MinValue
    val expectedTwo: Byte = 100
    val d = makeDeserializer(9, (x:ByteBuffer) => {
      x.putDouble(expectedOne)
      x.put(expectedTwo)
    })
    assert(d.deserialize_f64() == expectedOne)
    assert(d.deserialize_u8() == expectedTwo)
  }
  /*test("Deserializer.deserialize_str") {
    var expectedBytes = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).putLong( 4)
    val expected = new String("test".getBytes(), "utf-8")
    expectedBytes = expectedBytes.put(expected.getBytes())
    val d = new Deserializer(new ByteArrayInputStream(expectedBytes.array()))
    assert(d.deserialize_str() == "test")
  }*/

  def makeDeserializer(capacity: Int, insertVal: ByteBuffer=>ByteBuffer): Deserializer = {
    val byteRep = ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN)
    new Deserializer(new ByteArrayInputStream(insertVal(byteRep).array()))
  }
}
