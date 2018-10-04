import java.io.{PipedInputStream, PipedOutputStream}

import org.scalatest.FunSuite

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.sys.process._

class RustTest extends FunSuite {
  test("PipeOne") {
    val in = new PipedInputStream()
    val out = new PipedOutputStream(in)
    val retCode = Future {
      ("src\\test\\rust\\bincode-tester\\target\\release\\bincode-tester.exe write -a 1" #> out).!
    }
    val d = new Deserializer(in)
    val deserd = d.deserialize_struct[RandomStuff](new RandomStuffFactory)
  }
}
