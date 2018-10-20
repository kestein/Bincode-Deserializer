package com.kestein.deserializer.benchmarks

import java.util.concurrent.TimeUnit

import com.kestein.deserializer.{Deserializer, DeserializerException, DeserializerIterator}
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import scala.sys.process._

/* Benchmark of deserializing bincode data straight to a RandomStuff Object */

object DeserializeToObjectBenchmark {
  @State(Scope.Thread)
  class DataState {
    @Param(Array("1", "10", "100", "1000", "10000"))
    var iterations: Int = 1
    var iterationBincode: DeserializerIterator[RandomStuff] = _

    @Setup(Level.Iteration)
    def generateBincode(): Unit = {
      val exePath = "test-files/bin/bincode-tester.exe"
      s"$exePath write -a $iterations".run(new ProcessIO(
        _ => {
          // Unused
        },
        stdout => {
          iterationBincode = new DeserializerIterator[RandomStuff](
            new Deserializer(stdout),
            (d: Deserializer) => {
              d.deserialize_struct(new RandomStuffFactory)
                .fold(err => throw new DeserializerException(err),
                  rs => rs)
            })
        },
        _ => {
          // Unused
        }
      ))
      Thread.sleep(200)
    }
  }
}

@State(Scope.Thread)
@Threads(Threads.MAX)
class DeserializeToObjectBenchmark {
  import DeserializeToObjectBenchmark._

  @Benchmark
  @BenchmarkMode(Array(Mode.All))
  @Fork(1)
  @Measurement(iterations=12)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  @Warmup(iterations=10)
  def deserializeToObject(state: DataState, bh: Blackhole): Unit = {
    state.iterationBincode.foreach(rs => bh.consume(rs))
  }
}
