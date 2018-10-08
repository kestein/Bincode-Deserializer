import java.io.{IOException, InputStream}
import java.math.BigInteger
import java.nio.{ByteBuffer, ByteOrder}

class Deserializer(source: InputStream, endianness: ByteOrder = ByteOrder.LITTLE_ENDIAN) {
  type DeserializeResult[T] = Either[DeserializerError, T]

  var bytesRead = 0
  val MaxIntAsBigInt = BigInt.apply(Int.MaxValue)

  /* ========================================== Deserialization Functions ========================================== */

  def deserialize_bool(): DeserializeResult[Boolean] = {
    readSizedNumber(1).get(0) match {
      case 0 => Right(false)
      case 1 => Right(true)
      case i@_ => Left(new InvalidVariantError(i, Seq("0", "1")))
    }
  }

  /*
    Since there are no unsigned types the next size up is used for the corresponding bit size to ensure enough space
    for all possible values

    u8 -> Short
    i8 -> Byte
    u16 -> Int
    i16 -> Short
    u32 -> Long
    i32 -> Int
    u64 -> BigIng
    i64 -> Long
  */
  def deserialize_u8(): Short = {
    val rawBytes = readSizedNumber(1)
    rawBytes.getShort(0)
  }

  def deserialize_i8(): Byte = {
    val rawBytes = readSizedNumber(1)
    rawBytes.get(0)
  }

  def deserialize_u16(): Int = {
    val rawBytes = readSizedNumber(2)
    rawBytes.getInt(0)
  }

  def deserialize_i16(): Short = {
    val rawBytes = readSizedNumber(2)
    rawBytes.getShort(0)
  }

  def deserialize_u32(): Long = {
    val rawBytes = readSizedNumber(4)
    rawBytes.getLong(0)
  }

  def deserialize_i32(): Int = {
    val rawBytes = readSizedNumber(4)
    rawBytes.getInt(0)
  }

  def deserialize_u64(): BigInt = {
    val rawBytes = readSizedNumber(8)
    new BigInteger(rawBytes.array().reverse)
  }

  def deserialize_i64(): Long = {
    val rawBytes = readSizedNumber(8)
    rawBytes.getLong(0)
  }

  def deserialize_f32(): Float = {
    val rawBytes = readSizedNumber(4)
    rawBytes.getFloat(0)
  }

  def deserialize_f64(): Double = {
    val rawBytes = readSizedNumber(8)
    rawBytes.getDouble(0)
  }

  def deserialize_str(): String = {
    val length = deserialize_u64()
    bytesRead += 8
    val buf: Array[Byte] = allocateLargeArray(length)
    readLargeBytes(buf, length)
    new String(buf, "utf-8")
  }

  /*
    Deserialize an option type.

    @param deserFun: A function to apply on the source stream to retrieve the value
    @return: An option over the given type T
   */
  def deserialize_option[T](deserFun: Deserializer => T): Option[T] = {
    deserialize_u8() match {
      case 0 => None
      case 1 => Some(deserFun(this))
      case _ => throw new IOException("Invalid option encoding")
    }
  }

  def deserialize_seq[T](deserFun: Deserializer => T): Seq[T] = {
    // Grab the length of the sequence
    val length = deserialize_u64()
    var out = Seq.newBuilder[T]
    // Call the deserialization function length # of times
    for (_ <- BigInt.apply(0) until length) {
      out += deserFun(this)
    }
    out.result()
  }

  def deserialize_tuple[T](config: FlatDeserializeConfig[T]): T = {
    config.deserialize(this)
  }

  def deserialize_struct[T](config: FlatDeserializeConfig[T]): T = {
    deserialize_tuple(config)
  }

  def deserialize_object[T](config: FlatDeserializeConfig[T]): T = {
    deserialize_tuple(config)
  }

  def deserialize_map[K, V](config: MapDeserializeConfig[K, V]): Map[K, V] = {
    val values = deserialize_u64()
    var out: Map[K, V] = Map()
    for (_ <- BigInt(0) until values) {
      //out(config.deserialize_key(this)) = config.deserialize_value(this)
      out = out + (config.deserialize_key(this) -> config.deserialize_value(this))
    }
    out
  }

  def deserialize_enum[T](config: EnumDeserializeConfig[T]): T = {
    val variant = deserialize_u32()
    config.deserialize(variant, this)
  }
  /* =========================================== Helper Functions =========================================== */

  /*
    Reads the specified number of bytes into a buffer for the purposes of number creation. Increments the bytesRead cursor.

    @param amount: The amount of bytes to read from the buffer. Also is the size of the number to be read:
                      1 -> Byte (8 bit)
                      2 -> Short (16 bit
                      4 -> Int (32 bit)
                      8 - > Long (64 bit)
    @return: A ByteBuffer with the amount of bytes specified. The buffer will have double the specified amount of bytes
             allocated to ensure that unsigned numbers are read/converted properly
   */
  private def readSizedNumber(amount: Int): ByteBuffer = {
    /* Use the next byte size up */
    val jvmSize = amount*2
    val buf: Array[Byte] = Array.fill(jvmSize)(0)
    /* Read the actual requested amount from the underlying buffer */
    if (source.readNBytes(buf, 0, amount) != amount) {
      throw new IOException("Requested number of bytes is more than the number of bytes in the source")
    } else {
      bytesRead += amount
      ByteBuffer.allocate(jvmSize).order(endianness).put(buf)
    }
  }

  /*
    Create a byte array that can support 8 bit sizes

    @param size: The size of the buffer to allocate
    @return: An empty array with the specified size
   */
  private def allocateLargeArray(size: BigInt): Array[Byte] = {
    val ints: Int = (size/MaxIntAsBigInt).toInt
    if (ints == 0) {
      // The entire array can be made by typecasting
      Array.fill(size.toInt)(0)
    } else {
      // A larger array is needed. Append into one array the large length needed
      var out: Array[Byte] = new Array(0)
      for (_ <- 0 to ints) {
        out = out ++ Array.fill(Int.MaxValue)(0.toByte)
      }
      out = out ++ Array.fill((size%MaxIntAsBigInt).toInt)(0.toByte)
      out
    }
  }

  /*
    Reads the amount specified by length into the buf array. The buf must be preallocated for the requested number of
     bytes.
   */
  private def readLargeBytes(buf: Array[Byte], length: BigInt) = {
    assert(buf.length>=length)
    val reads: Int = (length/MaxIntAsBigInt).toInt
    val leftover: Int = (length%MaxIntAsBigInt).toInt
    if (reads == 0) {
      // The requested read can fit from one read
      source.readNBytes(buf, 0, length.toInt)
    } else {
      // A series of reads is used to fill the buffer
      for (i <- 0 to reads) {
        if (source.readNBytes(buf, i*Int.MaxValue, Int.MaxValue) != Int.MaxValue) {
          throw new IOException("Requested number of bytes is more than the number of bytes in the source")
        }
        bytesRead += Int.MaxValue
      }
      // Read in any leftover bytes
      if (source.readNBytes(buf, reads*Int.MaxValue, leftover) != leftover) {
        throw new IOException("Requested number of bytes is more than the number of bytes in the source")
      }
      bytesRead += leftover
    }
  }
}
