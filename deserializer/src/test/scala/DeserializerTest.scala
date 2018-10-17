import java.io.ByteArrayInputStream
import java.nio.{ByteBuffer, ByteOrder}

import com.kestein.deserializer.Deserializer.DeserializeResult
import com.kestein.deserializer._
import org.scalatest.FunSuite

class DeserializerTest extends FunSuite {
  /* Validate valid input yields a valid result */
  test("com.kestein.deserializer.Deserializer.deserialize_bool") {
    // False
    var d = makeDeserializer(1, (x:ByteBuffer) => x.put(0.toByte))
    var bool = d.deserialize_bool()
    assert(bool.isRight)
    assert(!bool.getOrElse(true))
    // True
    d = makeDeserializer(1, (x:ByteBuffer) => x.put(1.toByte))
    bool = d.deserialize_bool()
    assert(bool.isRight)
    assert(bool.getOrElse(false))
  }
  test("com.kestein.deserializer.Deserializer.deserialize_u8") {
    val expected = 97
    val d = makeDeserializer(1, (x:ByteBuffer) => x.put(expected.toByte))
    assert(d.deserialize_u8() == Right(expected.toShort))
  }
  test("com.kestein.deserializer.Deserializer.deserialize_i8") {
    val expected = -97
    val d = makeDeserializer(1, (x:ByteBuffer) => x.put(expected.toByte))
    assert(d.deserialize_i8() == Right(expected.toByte))
  }
  test("com.kestein.deserializer.Deserializer.deserialize_u16") {
    val expected = 300
    val d = makeDeserializer(2, (x:ByteBuffer) => x.putShort(expected.toShort))
    assert(d.deserialize_u16() == Right(expected.toInt))
  }
  test("com.kestein.deserializer.Deserializer.deserialize_i16") {
    val expected = -300
    val d = makeDeserializer(2, (x:ByteBuffer) => x.putShort(expected.toShort))
    assert(d.deserialize_i16() == Right(expected.toShort))
  }
  test("com.kestein.deserializer.Deserializer.deserialize_u32") {
    val expected = 300000
    val d = makeDeserializer(4, (x:ByteBuffer) => x.putInt(expected))
    assert(d.deserialize_u32() == Right(expected.toLong))
  }
  test("com.kestein.deserializer.Deserializer.deserialize_i32") {
    val expected = -300000
    val d = makeDeserializer(4, (x:ByteBuffer) => x.putInt(expected))
    assert(d.deserialize_i32() == Right(expected))
  }
  test("com.kestein.deserializer.Deserializer.deserialize_u64") {
    val expected: Long = Long.MaxValue
    val d = makeDeserializer(8, (x:ByteBuffer) => x.putLong(expected))
    assert(d.deserialize_u64() == Right(expected))
  }
  test("com.kestein.deserializer.Deserializer.deserialize_i64") {
    val expected = Long.MinValue
    val d = makeDeserializer(8, (x:ByteBuffer) => x.putLong(expected))
    assert(d.deserialize_i64() == Right(expected))
  }
  test("com.kestein.deserializer.Deserializer.deserialize_f32") {
    val expected = Float.MinValue
    val d = makeDeserializer(8, (x:ByteBuffer) => x.putFloat(expected))
    assert(d.deserialize_f32() == Right(expected))
  }
  test("com.kestein.deserializer.Deserializer.deserialize_f64") {
    val expected = Double.MinValue
    val d = makeDeserializer(8, (x:ByteBuffer) => x.putDouble(expected))
    assert(d.deserialize_f64() == Right(expected))
  }
  test("com.kestein.deserializer.Deserializer.deserialize_multiple_numbers") {
    val expectedOne = Double.MinValue
    val expectedTwo: Byte = 100
    val d = makeDeserializer(9, (x:ByteBuffer) => {
      x.putDouble(expectedOne)
      x.put(expectedTwo)
    })
    assert(d.deserialize_f64() == Right(expectedOne))
    assert(d.deserialize_u8() == Right(expectedTwo))
  }
  test("com.kestein.deserializer.Deserializer.deserialize_str") {
    var expectedBytes = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
    expectedBytes = putString(expectedBytes, "test")
    val d = new Deserializer(new ByteArrayInputStream(expectedBytes.array()))
    assert(d.deserialize_str() == Right("test"))
  }
  test("com.kestein.deserializer.Deserializer.deserialize_none") {
    val expected = None
    val d = makeDeserializer(1, (x:ByteBuffer) => x.put(0.toByte))
    assert(d.deserialize_option((x:Deserializer) => x.deserialize_u8()) == Right(expected))
  }
  test("com.kestein.deserializer.Deserializer.deserialize_some_int") {
    val expected: Some[Int] = Some(7)
    val d = makeDeserializer(5, (x:ByteBuffer) => x.put(1.toByte).putInt(7))
    assert(d.deserialize_option((x:Deserializer) => x.deserialize_u32()) == Right(expected))
  }
  test("com.kestein.deserializer.Deserializer.deserialize_some_int_some_short") {
    val expectedOne: Some[Int] = Some(7)
    val expectedTwo: Some[Short] = Some(-1)
    // Make the deserializer
    var byteRep = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
    byteRep = byteRep.put(1.toByte).putInt(7) // 5
    byteRep = byteRep.put(1.toByte).putShort(-1) // 3
    val d = new Deserializer(new ByteArrayInputStream(byteRep.array()))
    // Retrieve the values
    assert(d.deserialize_option((x:Deserializer) => x.deserialize_u32()) == Right(expectedOne))
    assert(d.deserialize_option((x:Deserializer) => x.deserialize_i16()) == Right(expectedTwo))
  }
  test("com.kestein.deserializer.Deserializer.deserialize_seq") {
    val expected: Array[Int] = Array(1,2,3,4,5)
    // Make the deserializer
    var byteRep = ByteBuffer.allocate((5*4)+8).order(ByteOrder.LITTLE_ENDIAN)
    byteRep = byteRep.putLong(5)
    for(i <- 1 to 5) {
      byteRep = byteRep.putInt(i)
    }
    val d = new Deserializer(new ByteArrayInputStream(byteRep.array()))
    // Retrieve the values
    d.deserialize_seq((x:Deserializer) => x.deserialize_i32()).fold(err => throw new DeserializerException(err), s => {
      assert(s.toArray sameElements expected)
    })
  }
  test("com.kestein.deserializer.Deserializer.deserialize_tuple") {
    val expected = (9, "one", 8.toLong)
    var byteRep = ByteBuffer.allocate((5*4)+8).order(ByteOrder.LITTLE_ENDIAN)
    byteRep = byteRep.put(9.toByte)
    byteRep = putString(byteRep, "one")
    byteRep = byteRep.putLong(8)
    val d = new Deserializer(new ByteArrayInputStream(byteRep.array()))

    object ByteStringLongConfig extends FlatDeserializeConfig[(Byte, String, Long)] {
      override def deserialize(d: Deserializer): DeserializeResult[(Byte, String, Long)] = {
        d.deserialize_i8().fold(err => Left(err), one => {
          d.deserialize_str().fold(err => Left(err), two => {
            d.deserialize_i64().map(three => {
              return Right(one, two, three)
            })
          })
        })
      }
    }
    assert(d.deserialize_tuple[(Byte, String, Long)](ByteStringLongConfig) == Right(expected))
  }
  test("com.kestein.deserializer.Deserializer.deserialize_map") {
    val expected: Map[Int, Int] = Map(1->2,3->4,5->6)
    var byteRep = ByteBuffer.allocate(8+6*4).order(ByteOrder.LITTLE_ENDIAN)
    byteRep = byteRep.putLong(3)
    for (i <- 1 to 6) {
      byteRep = byteRep.putInt(i)
    }
    object MapIntIntConfig extends MapDeserializeConfig[Int, Int] {
      def deserialize_key(d: Deserializer): DeserializeResult[Int] = {
        d.deserialize_i32()
      }
      def deserialize_value(d: Deserializer): DeserializeResult[Int] = {
        d.deserialize_i32()
      }
    }
    val d = new Deserializer(new ByteArrayInputStream(byteRep.array()))
    assert(d.deserialize_map(MapIntIntConfig) == Right(expected))
  }
  test("com.kestein.deserializer.Deserializer.deserialize_enum") {
    object TesterEnum extends Enumeration {
      type TesterEnum = Int
      val pass = 100
      val fail = 99
    }
    val d = makeDeserializer(12, (x:ByteBuffer) => {
      var y = x.putInt(0)
      y = y.putInt(1)
      y
    })
    object TesterEnumDeserializeConfig extends EnumDeserializeConfig[TesterEnum.TesterEnum] {
      override def deserialize(variant: Long, d: Deserializer): DeserializeResult[TesterEnum.TesterEnum] = {
        variant match {
          case 0 => Right(TesterEnum.pass)
          case 1 => Right(TesterEnum.fail)
          case v@_ => Left(new InvalidVariantError(v.toInt, Seq("0", "1")))
        }
      }
    }
    assert(d.deserialize_enum(TesterEnumDeserializeConfig) == Right(TesterEnum.pass))
    assert(d.deserialize_enum(TesterEnumDeserializeConfig) == Right(TesterEnum.fail))
  }
  test("com.kestein.deserializer.Deserializer.deserialize_big_endian") {
    val expected = 1 << 8
    var byteRep = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
    byteRep = byteRep.putShort(1)
    val d = new Deserializer(new ByteArrayInputStream(byteRep.array()), ByteOrder.BIG_ENDIAN)
    assert(d.deserialize_i16().right.get == expected.toShort)
  }

  /* Validate invalid input yields an error */
  test("com.kestein.deserializer.Deserializer.deserialize_bool_invalid") {
    val d = makeDeserializer(1, (x:ByteBuffer) => x.put(9.toByte))
    val bool = d.deserialize_bool()
    assert(bool.isLeft)
    assert(bool.left.get.isInstanceOf[InvalidVariantError])
  }
  test("com.kestein.deserializer.Deserializer.deserialize_u64_invalid") {
    val d = makeDeserializer(1, (x:ByteBuffer) => x.put(9.toByte))
    val bool = d.deserialize_u64()
    assert(bool.isLeft)
    assert(bool.left.get.isInstanceOf[EOFError])
  }
  test("com.kestein.deserializer.Deserializer.deserialize_str_invalid") {
    var expectedBytes = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
    expectedBytes = expectedBytes.putLong(500)
    expectedBytes = expectedBytes.put("test".getBytes("utf-8"))
    val d = new Deserializer(new ByteArrayInputStream(expectedBytes.array()))
    val st = d.deserialize_str()
    assert(st.isLeft)
    assert(st.left.get.isInstanceOf[EOFError])
  }
  test("com.kestein.deserializer.Deserializer.deserialize_option_invalid") {
    val d = makeDeserializer(1, (x:ByteBuffer) => x.put(10.toByte))
    val opt = d.deserialize_option((x:Deserializer) => x.deserialize_u8())
    assert(opt.isLeft)
    assert(opt.left.get.isInstanceOf[InvalidVariantError])
  }

  /* Helper functions */
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
