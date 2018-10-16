import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.fasterxml.jackson.databind.ObjectMapper
import org.scalameter.api.RegressionReporter
import org.scalameter.execution.SeparateJvmsExecutor
import org.scalameter.persistence.JSONSerializationPersistor
import org.scalameter.picklers.Implicits._
import org.scalameter.reporting.DsvReporter
import org.scalameter._

import scala.sys.process._

/* Benchmark of taking the data as a string and loading it as a JsonNode.
 * Does not measure the time it takes to convert the data from bincode to json.*/
object ConvertAndDeserializeToJsonBenchmark extends Bench[Double] {
  // Bencher config
  lazy val measurer = new Measurer.Default
  lazy val reporter = Reporter.Composite(
    new RegressionReporter(
      RegressionReporter.Tester.OverlapIntervals(),
      RegressionReporter.Historian.ExponentialBackoff() ),
    DsvReporter(',')
  )
  lazy val persistor = new JSONSerializationPersistor()
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
  performance of "ConvertAndDeserializeToJson" in {
    measure method "RustConvert.foreach.ObjectMapper.readTree" in {
      using(readers) in {
        reader => {
          ("src\\test\\rust\\bincode-tester\\target\\release\\bincode-tester.exe convert" #< reader).lineStream.iterator.foreach(s => mapper.readTree(s))
        }
      }
    }
  }
}
