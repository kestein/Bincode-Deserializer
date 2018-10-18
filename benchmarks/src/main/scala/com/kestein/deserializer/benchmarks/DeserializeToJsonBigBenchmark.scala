package com.kestein.deserializer.benchmarks

import java.io.{BufferedReader, File, FileReader}
import java.util.concurrent.TimeUnit

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.openjdk.jmh.annotations.Mode.All
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

object DeserializeToJsonBigBenchmark {
  @State(Scope.Thread)
  class DataState {
    @Param(Array("5000", "10000"))
    var iterations: Int = 1
    var jsonData: BufferedReader = _
    final val om = new ObjectMapper
    om.registerModule(DefaultScalaModule)

    /* Make JSON data once */
    @Setup(Level.Iteration)
    def generateJson(): Unit = {
      val filename: String = "test-files/data/" + iterations + "-records.json"
      jsonData = new BufferedReader(new FileReader(new File(filename)))
    }
  }
}

@State(Scope.Thread)
class DeserializeToJsonBigBenchmark {
  import DeserializeToJsonBigBenchmark._

  @Benchmark
  @BenchmarkMode(Array(All))
  @Fork(1)
  @Measurement(iterations=12)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  @Warmup(iterations=10)
  def deserializeToJsonBig(state: DataState, bh: Blackhole): Unit = {
    state.jsonData.lines().forEach(line => bh.consume(state.om.readTree(line)))
  }
}
