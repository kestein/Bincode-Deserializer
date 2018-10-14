import com.fasterxml.jackson.databind.ObjectMapper
import org.scalameter
import org.scalameter.api.LoggingReporter
import org.scalameter._
import org.scalameter.execution.SeparateJvmsExecutor
import org.scalameter.picklers.Implicits._

import scala.sys.process._

/* Benchmark of taking the data as a string and loading it as a JsonNode.
 * Does not measure the time it takes to convert the data from bincode to json.*/
object DeserializeToJsonBenchmark extends Bench[Double] {
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
  val readers: Gen[Iterator[String]] = for (amount <- records) yield {
    val jsonStream = (s"src\\test\\rust\\bincode-tester\\target\\release\\bincode-tester.exe write -a $amount" #|
        s"src\\test\\rust\\bincode-tester\\target\\release\\bincode-tester.exe convert ").lineStream
    jsonStream.iterator
  }
  final val mapper = new ObjectMapper
  performance of "ObjectMapper" in {
    measure method "readTree" in {
      using(readers) in {
        reader => {
          reader.foreach(s => mapper.readTree(s))
        }
      }
    }
  }
}
