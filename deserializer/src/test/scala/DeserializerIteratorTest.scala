import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.kestein.deserializer.{Deserializer, DeserializerException, DeserializerIterator}
import org.scalatest.FunSuite

import scala.sys.process._

class DeserializerIteratorTest extends FunSuite {
  test("com.kestein.deserializer.Deserializer.iterator") {
    val amount = 20
    val stdout = new ByteArrayOutputStream()
    (s"${RustTest.bincodeTesterPath} write -a $amount" #> stdout).!
    val d = new Deserializer(new ByteArrayInputStream(stdout.toByteArray))
    assert(d.available() > 0)
    val dIter = new DeserializerIterator[RandomStuff](d, (d: Deserializer) =>{
      d.deserialize_struct(new RandomStuffFactory)
       .fold(err => throw new DeserializerException(err), rs => rs)
    })
    assert(dIter.nonEmpty)
    assert(dIter.toArray.length == amount)
  }
}
