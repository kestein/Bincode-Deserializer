import java.io.{File, PipedInputStream, PipedOutputStream}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.sys.process._
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.scalatest.FunSuite

class RustTest extends FunSuite {
  test("PipeOne") {
    val in = new PipedInputStream()
    val out = new PipedOutputStream(in)
    val _ = Future {
      ("src\\test\\rust\\bincode-tester\\target\\release\\bincode-tester.exe write -a 1" #> out).!
    }
    val d = new Deserializer(in)
    val deserializedValue: RandomStuff = d.deserialize_struct[RandomStuff](new RandomStuffFactory)
    val om = new ObjectMapper()
    om.registerModule(DefaultScalaModule)

    om.writeValue(new File("from-scala.json"), deserializedValue)
  }
}
