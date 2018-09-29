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
  test("Deserializer.deserialize_str") {
    /*var expectedBytes = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).putLong( 4)
    val expected = new String("test".getBytes(), "utf-8")
    expectedBytes = expectedBytes.put(expected.getBytes())*/
    var expectedBytes = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
    expectedBytes = putString(expectedBytes, "test")
    val d = new Deserializer(new ByteArrayInputStream(expectedBytes.array()))
    assert(d.deserialize_str() == "test")
  }
  test("Deserializer.deserialize_none") {
    val expected = None
    val d = makeDeserializer(1, (x:ByteBuffer) => x.put(0.toByte))
    assert(d.deserialize_option((x:Deserializer) => x.deserialize_u8()) == expected)
  }
  test("Deserializer.deserialize_some_int") {
    val expected: Some[Int] = Some(7)
    val d = makeDeserializer(5, (x:ByteBuffer) => x.put(1.toByte).putInt(7))
    assert(d.deserialize_option((x:Deserializer) => x.deserialize_u32()) == expected)
  }
  test("Deserializer.deserialize_some_int_some_short") {
    val expectedOne: Some[Int] = Some(7)
    val expectedTwo: Some[Short] = Some(-1)
    // Make the deserializer
    var byteRep = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
    byteRep = byteRep.put(1.toByte).putInt(7) // 5
    byteRep = byteRep.put(1.toByte).putShort(-1) // 3
    val d = new Deserializer(new ByteArrayInputStream(byteRep.array()))
    // Retrieve the values
    assert(d.deserialize_option((x:Deserializer) => x.deserialize_u32()) == expectedOne)
    assert(d.deserialize_option((x:Deserializer) => x.deserialize_i16()) == expectedTwo)
  }
  test("Deserializer.deserialize_seq") {
    val expected: Array[Int] = Array(1,2,3,4,5)
    // Make the deserializer
    var byteRep = ByteBuffer.allocate((5*4)+8).order(ByteOrder.LITTLE_ENDIAN)
    byteRep = byteRep.putLong(5)
    for(i <- 1 to 5) {
      byteRep = byteRep.putInt(i)
    }
    val d = new Deserializer(new ByteArrayInputStream(byteRep.array()))
    // Retrieve the values
    assert(d.deserialize_seq((x:Deserializer) => x.deserialize_i32()).toArray sameElements expected)
  }
  test("Deserializer.deserialize_tuple") {
    val expected = (9, "one", 8.toLong)
    var byteRep = ByteBuffer.allocate((5*4)+8).order(ByteOrder.LITTLE_ENDIAN)
    byteRep = byteRep.put(9.toByte)
    byteRep = putString(byteRep, "one")
    byteRep = byteRep.putLong(8)
    val d = new Deserializer(new ByteArrayInputStream(byteRep.array()))

    object SampleConfig extends TupleDeserializeConfig[(Byte, String, Long)] {
      override def deserialize(d: Deserializer): (Byte, String, Long) = {
        (d.deserialize_i8(), d.deserialize_str(), d.deserialize_i64())
      }
    }
    assert(d.deserialize_tuple[(Byte, String, Long)](SampleConfig) == expected)
  }

  def makeDeserializer(capacity: Int, insertVal: ByteBuffer=>ByteBuffer): Deserializer = {
    val byteRep = ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN)
    new Deserializer(new ByteArrayInputStream(insertVal(byteRep).array()))
  }

  def putString(buffer: ByteBuffer, str: String): ByteBuffer = {
    var newBuf = buffer.putLong(str.length)
    newBuf = newBuf.put(str.getBytes("utf-8"))
    newBuf
  }
}
