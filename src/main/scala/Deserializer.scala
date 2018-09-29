import java.io.{IOException, InputStream}
import java.nio.{ByteBuffer, ByteOrder}

import scala.io.Codec.fromUTF8
import scala.util.control.Exception._

class Deserializer(source: InputStream, endianness: ByteOrder = ByteOrder.LITTLE_ENDIAN) {
  var bytesRead = 0

  def deserialize_bool(): Boolean = {
    if (readSizedNumber(1).get(0) == 0) false else true
  }

  /*
    Since there are no unsigned types the next size up is used for the corresponding bit size to ensure enough space
    for all possible values
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
    BigInt.apply(rawBytes.getLong(0))
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
    /*val length = readSizedNumber(8).getLong()
    bytesRead += 8
    var buf: Array[Byte] = allocateLongArray(length)
    readLongBytes(buf, length)
    if (source.readNBytes(buf, bytesRead, length) != length) {
      throw new IOException("Requested number of bytes is more than the number of bytes in the source")
    } else {
      bytesRead += length
      new String(buf, "utf-8")
    }*/
    ""
  }

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
}
