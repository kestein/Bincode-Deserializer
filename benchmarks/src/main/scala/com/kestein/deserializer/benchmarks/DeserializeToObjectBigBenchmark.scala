package com.kestein.deserializer.benchmarks

import java.io.{File, FileInputStream}
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import com.kestein.deserializer.{Deserializer, DeserializerException, DeserializerIterator}

object DeserializeToObjectBigBenchmark {
  @State(Scope.Thread)
  class DataState {
    @Param(Array("5000", "10000"))
    var iterations: Int = 1
    var iterationBincode: DeserializerIterator[RandomStuff] = _

    /* Make Bincode data once */
    @Setup(Level.Iteration)
    def generateBincode(): Unit = {
      val filename = "test-files/data/" + iterations + "-records.bin"
      val binFile = new FileInputStream(new File(filename))
      iterationBincode = new DeserializerIterator[RandomStuff](
        new Deserializer(binFile),
        (d: Deserializer) => {
          d.deserialize_struct(new RandomStuffFactory)
            .fold(err => throw new DeserializerException(err),
              rs => rs)
        })
    }
  }
}

@State(Scope.Thread)
class DeserializeToObjectBigBenchmark {
  import DeserializeToObjectBigBenchmark.DataState

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
