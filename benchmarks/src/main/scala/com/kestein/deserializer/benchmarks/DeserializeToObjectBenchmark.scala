package com.kestein.deserializer.benchmarks

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.concurrent.TimeUnit

import com.kestein.deserializer.{Deserializer, DeserializerException, DeserializerIterator}
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import scala.sys.process._

object DeserializeToObjectBenchmark {
  @State(Scope.Thread)
  class DataState {
    @Param(Array("1", "10", "100", "1000", "2500"))
    var iterations: Int = 1
    var generatedBincode: Array[Byte] = _
    var iterationBincode: DeserializerIterator[RandomStuff] = _

    /* Make Bincode data once */
    @Setup(Level.Trial)
    def generateBincode(): Unit = {
      val exePath = "test-files/bin/bincode-tester.exe"
      val stdout = new ByteArrayOutputStream()
      (s"$exePath write -a $iterations" #> stdout).!
      generatedBincode = stdout.toByteArray
    }

    /* Make copies of the already generated Bincode */
    @Setup(Level.Iteration)
    def generateIterationBincode(): Unit = {
      val iterationBincodeBytes: Array[Byte] = new Array(generatedBincode.length)
      generatedBincode.copyToArray(iterationBincodeBytes)
      iterationBincode = new DeserializerIterator[RandomStuff](new Deserializer(new ByteArrayInputStream(iterationBincodeBytes)),
                                                               (d: Deserializer) => {
                                                                 d.deserialize_struct(new RandomStuffFactory)
                                                                  .fold(err => throw new DeserializerException(err),
                                                                        rs => rs)
                                                               })
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
