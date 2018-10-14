import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import org.scalameter
import org.scalameter.api.LoggingReporter
import org.scalameter._
import org.scalameter.execution.{LocalExecutor, SeparateJvmsExecutor}
import org.scalameter.picklers.Implicits._

import scala.sys.process._

/* Benchmark of taking the data as a string and loading it as a JsonNode.
 * Does not measure the time it takes to convert the data from bincode to json.*/
object ConvertAndDeserializeToJsonBenchmark extends Bench[Double] {
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
  val end = 100
  val records: Gen[Int] = Gen.exponential("records")(start, end, 10)
  val readers: Gen[ByteArrayInputStream] = for (amount <- records) yield {
    val byteOutput = new ByteArrayOutputStream()
    (s"src\\test\\rust\\bincode-tester\\target\\release\\bincode-tester.exe write -a $amount" #> byteOutput).!
    new ByteArrayInputStream(byteOutput.toByteArray)
  }
  final val mapper = new ObjectMapper
  performance of "Deserializer" in {
    measure method "readTree" in {
      using(readers) in {
        reader => {
          ("src\\test\\rust\\bincode-tester\\target\\release\\bincode-tester.exe convert" #< reader).lineStream.iterator.foreach(s => mapper.readTree(s))
        }
      }
    }
  }
}
