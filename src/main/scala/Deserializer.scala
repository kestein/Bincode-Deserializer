import java.io.InputStream
import java.nio.{ByteOrder, ByteBuffer}

class Deserializer(source: InputStream, endianness: ByteOrder = ByteOrder.LITTLE_ENDIAN) {
  var bytesRead = 0
  /*
    Since there are no unsigned types the next size up is used for the corresponding bit size to ensure enough space
    for all possible values
  */
  def deserialize_u8(): Short = {
    val rawBytes = grabN(1)
    rawBytes.getShort(0)
  }

  def deserialize_i8(): Byte = {
    val rawBytes = grabN(1)
    rawBytes.get(0)
  }

  def deserialize_u16(): Int = {
    val rawBytes = grabN(2)
    rawBytes.getInt(0)
  }

  def deserialize_i16(): Short = {
    val rawBytes = grabN(2)
    rawBytes.getShort(0)
  }

  def deserialize_u32(): Long = {
    val rawBytes = grabN(4)
    rawBytes.getLong(0)
  }

  def deserialize_i32(): Int = {
    val rawBytes = grabN(4)
    rawBytes.getInt(0)
  }

  def deserialize_u64(): BigInt = {
    val rawBytes = grabN(8)
    BigInt.apply(rawBytes.getLong(0))
  }

  def deserialize_i64(): Long = {
    val rawBytes = grabN(8)
    rawBytes.getLong(0)
  }

  def deserialize_f32(): Float = {
    val rawBytes = grabN(4)
    rawBytes.getFloat(0)
  }

  def deserialize_f64(): Double = {
    val rawBytes = grabN(8)
    rawBytes.getDouble(0)
  }

  def grabN(amount: Int): ByteBuffer = {
    /* Use the next byte size up */
    val jvmSize = amount*2
    val buf: Array[Byte] = Array.fill(jvmSize)(0)
    /* Read the actual requested amount from the underlying buffer */
    if (source.readNBytes(buf, bytesRead, amount) != amount) {
      // TODO: do some error here
      ByteBuffer.allocate(0).order(endianness)
    } else {
      bytesRead += amount
      ByteBuffer.allocate(jvmSize).order(endianness).put(buf)
    }
  }
}
