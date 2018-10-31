package com.kestein.deserializer

/*
    Wrapper class to make a Deserializer iteratable.
   */
class DeserializerIterator[U](deserializer: Deserializer, deserializeDef: Deserializer=>U) extends Iterable[U] {
  override def iterator: Iterator[U] = new Iterator[U] {
    override def hasNext: Boolean = {
      deserializer.mark()
      val more = deserializer.read() != -1
      deserializer.reset()
      more
    }

    override def next(): U = {
      deserializeDef(deserializer)
    }
  }
}
