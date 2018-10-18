package com.kestein.deserializer.benchmarks

import java.util.concurrent.TimeUnit

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.openjdk.jmh.annotations.Mode._
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import scala.sys.process._

/* Benchmark of taking the data as a string and loading it as a JsonNode.
 * Does not measure the time it takes to convert the data from bincode to json.*/

object DeserializeToJsonBenchmark {
  @State(Scope.Thread)
  class DataState {
    @Param(Array("1", "10", "100", "1000", "2500"))
    var iterations: Int = 1
    var generatedJsonData: Array[String] = _
    var generatedJsonIterator: Array[String] = _
    final val om = new ObjectMapper
    om.registerModule(DefaultScalaModule)

    /* Make JSON data once */
    @Setup(Level.Trial)
    def generateJson(): Unit = {
      val exePath = "test-files/bin/bincode-tester.exe"
      generatedJsonData = (s"$exePath write -a $iterations" #| s"$exePath convert ").lineStream.toArray
    }

    /* Make copies of the already generated JSON */
    @Setup(Level.Iteration)
    def generateJsonIterator(): Unit = {
      generatedJsonIterator = new Array(generatedJsonData.length)
      generatedJsonData.copyToArray(generatedJsonIterator)
    }
  }
}

@State(Scope.Thread)
@Threads(Threads.MAX)
class DeserializeToJsonBenchmark extends App {
  import DeserializeToJsonBenchmark._

  @Benchmark
  @BenchmarkMode(Array(All))
  @Fork(1)
  @Measurement(iterations=12)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  @Warmup(iterations=10)
  def deserializeToJson(state: DataState, bh: Blackhole): Unit = {
    state.generatedJsonIterator.foreach(line => bh.consume(state.om.readTree(line)))
  }
}
