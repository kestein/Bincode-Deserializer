package com.kestein.deserializer

import java.io.InputStream
import java.nio.{ByteBuffer, ByteOrder}

import com.kestein.deserializer.Deserializer.DeserializeResult

class Deserializer(source: InputStream, endianness: ByteOrder = ByteOrder.LITTLE_ENDIAN) {
  var bytesRead = 0
  val MaxIntAsBigInt = BigInt.apply(Int.MaxValue)

  /* ========================================== Deserialization Functions ========================================== */

  def deserialize_bool(): DeserializeResult[Boolean] = {
    readSizedNumber(1).fold[DeserializeResult[Boolean]](err => Left(err), boolBuf =>{
      boolBuf.get(0) match {
        case 0 => Right(false)
        case 1 => Right(true)
        case a@_ => Left(new InvalidVariantError(a, Seq("0", "1")))
      }
    })
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
  def deserialize_u8(): DeserializeResult[Short] = {
    readSizedNumber(1).map(byteBuf => byteBuf.getShort(0))
    //rawBytes.getShort(0)
  }

  def deserialize_i8(): DeserializeResult[Byte] = {
    readSizedNumber(1).map(byteBuf => byteBuf.get(0))
  }

  def deserialize_u16(): DeserializeResult[Int] = {
    readSizedNumber(2).map(byteBuf => byteBuf.getInt(0))
  }

  def deserialize_i16(): DeserializeResult[Short] = {
    readSizedNumber(2).map(byteBuf => byteBuf.getShort(0))
  }

  def deserialize_u32(): DeserializeResult[Long] = {
    readSizedNumber(4).map(byteBuf => byteBuf.getLong(0))
  }

  def deserialize_i32(): DeserializeResult[Int] = {
    readSizedNumber(4).map(byteBuf => byteBuf.getInt(0))
  }

  def deserialize_u64(): DeserializeResult[BigInt] = {
    readSizedNumber(8).map(byteBuf => BigInt(byteBuf.array().reverse))
  }

  def deserialize_i64(): DeserializeResult[Long] = {
    readSizedNumber(8).map(byteBuf => byteBuf.getLong(0))
  }

  def deserialize_f32(): DeserializeResult[Float] = {
    readSizedNumber(4).map(byteBuf => byteBuf.getFloat(0))
  }

  def deserialize_f64(): DeserializeResult[Double] = {
    readSizedNumber(8).map(byteBuf => byteBuf.getDouble(0))
  }

  def deserialize_str(): DeserializeResult[String] = {
    deserialize_u64().map(length => {
      bytesRead += 8
      val buf: Array[Byte] = allocateLargeArray(length)
      readLargeBytes(buf, length) match {
        case Some(err) => return Left(err)
        case None => return Right(new String(buf, "utf-8"))
      }
    })
  }

  /*
    Deserialize an option type.

    @param deserFun: A function to apply on the source stream to retrieve the value
    @return: An option over the given type T
   */
  def deserialize_option[T](deserFun: Deserializer => DeserializeResult[T]): DeserializeResult[Option[T]] = {
     deserialize_u8() map {
      case 0 => return Right(None)
      case 1 => deserFun(this).fold(err => return Left(err), value => return Right(Some(value)))
      case v@_ => return Left(new InvalidVariantError(v, Seq("0", "1")))
    }
  }

  def deserialize_seq[T](deserFun: Deserializer => DeserializeResult[T]): DeserializeResult[Seq[T]] = {
    // Grab the length of the sequence
    deserialize_u64().map(len => {
      var out = Seq.newBuilder[T]
      // Call the deserialization function length # of times
      for (_ <- BigInt.apply(0) until len) {
        out += deserFun(this).fold(err => return Left(err), seqVal => seqVal)
      }
      return Right(out.result())
    })
  }

  def deserialize_tuple[T](config: FlatDeserializeConfig[T]): DeserializeResult[T] = {
    config.deserialize(this)
  }

  def deserialize_struct[T](config: FlatDeserializeConfig[T]): DeserializeResult[T] = {
    deserialize_tuple(config)
  }

  def deserialize_object[T](config: FlatDeserializeConfig[T]): DeserializeResult[T] = {
    deserialize_tuple(config)
  }

  def deserialize_map[K, V](config: MapDeserializeConfig[K, V]): DeserializeResult[Map[K, V]] = {
    deserialize_u64().map(values => {
      var out: Map[K, V] = Map()
      for (_ <- BigInt(0) until values) {
        config.deserialize_entry(this).map(kv => {
          out = out + (kv._1 -> kv._2)
        })
      }
      return Right(out)
    })
  }

  def deserialize_enum[T](config: EnumDeserializeConfig[T]): DeserializeResult[T] = {
    deserialize_u32().map(variant => {
      return config.deserialize(variant, this)
    })
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
  private def readSizedNumber(amount: Int): DeserializeResult[ByteBuffer] = {
    /* Use the next byte size up */
    val jvmSize = amount*2
    val buf: Array[Byte] = Array.fill(jvmSize)(0)
    /* Read the actual requested amount from the underlying buffer */
    val actualRead = source.readNBytes(buf, 0, amount)
    if (actualRead != amount) {
      Left(checkReadError(amount, actualRead))
    } else {
      bytesRead += amount
      Right(ByteBuffer.allocate(jvmSize).order(endianness).put(buf))
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
  private def readLargeBytes(buf: Array[Byte], length: BigInt): Option[DeserializerError] = {
    assert(buf.length >= length)
    val reads: Int = (length / MaxIntAsBigInt).toInt
    val leftover: Int = (length % MaxIntAsBigInt).toInt
    var actual = 0
    if (reads == 0) {
      // The requested read can fit from one read
      actual = source.readNBytes(buf, 0, length.toInt)
      if (actual != length) {
        Some(checkReadError(length.toInt, actual))
      } else {
        None
      }
    } else {
      // A series of reads is used to fill the buffer
      for (i <- 0 to reads) {
        actual = source.readNBytes(buf, i * Int.MaxValue, Int.MaxValue)
        if (actual != Int.MaxValue) {
          Some(checkReadError(Int.MaxValue, actual))
        }
        bytesRead += Int.MaxValue
      }
      // Read in any leftover bytes
      actual = source.readNBytes(buf, reads * Int.MaxValue, leftover)
      if (actual != leftover) {
        Some(checkReadError(leftover, actual))
      }
      bytesRead += leftover
      None
    }
  }

  /*
    Determines what kind of IO error should be thrown

    @param requested: The number of bytes that was requested
    @param got: The number of bytes that were actually received
    @return: A com.kestein.deserializer.DeserializerError encapsulating what IO error happened
   */
  private def checkReadError(requested: Int, got: Int): DeserializerError = {
    if (source.available() == 0) {
      source.close()
      new EOFError
    } else {
      new DeserializeIOError(requested, got)
    }
  }

  /* Wrappers used for the sake of determining EOF when iterating. */

  /*
    Wrapper around Inputstream.mark().
   */
  def mark(): Unit = {
    source.mark(2)
  }

  /*
    Wrapper around Inputstream.read().
   */
  def read(): Int = {
    source.read()
  }

  /*
    Wrapper around Inputstream.read().
   */
  def reset(): Unit = {
    source.reset()
  }

  /*
    Turns this deserializer into an iterator over the deserialized values

    @param deserializeDef: The function to call to deserialize the data
    @return: A new DeserializerIterator over the specified type
   */
  def iterator[U](deserializeDef: Deserializer=>U): DeserializerIterator[U] = {
    new DeserializerIterator[U](this, deserializeDef)
  }
}

object Deserializer {
  type DeserializeResult[T] = Either[DeserializerError, T]
}
