import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import org.scalameter
import org.scalameter._
import org.scalameter.api.LoggingReporter
import org.scalameter.execution.SeparateJvmsExecutor
import org.scalameter.picklers.Implicits._

import scala.sys.process._

object DeserializeToObjectBenchmark extends Bench[Double] {
  // Bencher config
  lazy val measurer = new scalameter.Measurer.Default
  lazy val reporter = new LoggingReporter[Double]
  lazy val persistor: Persistor = Persistor.None
  lazy val executor = SeparateJvmsExecutor(
    new Executor.Warmer.Default,
    Aggregator.average,
    measurer)
  // Benchmark config
  val start = 1
  // ~6 minutes to generate 1000 json records, even longer above that
  val end = 10000
  val records: Gen[Int] = Gen.exponential("records")(start, end, 10)
  val readers: Gen[ByteArrayInputStream] = for (amount <- records) yield {
    val byteOutput = new ByteArrayOutputStream()
    (s"src\\test\\rust\\bincode-tester\\target\\release\\bincode-tester.exe write -a $amount" #> byteOutput).!
    new ByteArrayInputStream(byteOutput.toByteArray)
  }
  performance of "Deserializer" in {
    measure method "deserialize" in {
      using(readers) in {
        reader => {
          val d = new Deserializer(reader)
          while({
            val rs = d.deserialize_struct[RandomStuff](new RandomStuffFactory)
            rs.isRight
          }) {}
        }
      }
    }
  }
}
