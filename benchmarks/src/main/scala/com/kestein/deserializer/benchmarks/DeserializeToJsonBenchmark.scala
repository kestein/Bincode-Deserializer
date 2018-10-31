package com.kestein.deserializer.benchmarks

import java.io.{BufferedReader, InputStreamReader}
import java.util.concurrent.TimeUnit

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import scala.sys.process._

/* Benchmark of taking the data as a string and loading it as a JsonNode. */

object DeserializeToJsonBenchmark {
  @State(Scope.Thread)
  class DataState {
    @Param(Array("1", "100", "10000", "1000000"))
    var iterations: Int = 1
    var jsonData: BufferedReader = _
    final val om = new ObjectMapper
    om.registerModule(DefaultScalaModule)

    /* Make JSON data once */
    @Setup(Level.Iteration)
    def generateJson(): Unit = {
      val exePath = "test-files/bin/bincode-tester.exe"
      (s"$exePath write -a $iterations" #| s"$exePath convert").run(new ProcessIO(
        _ => {
          // Unused
        },
        stdout => {
          jsonData = new BufferedReader(new InputStreamReader(stdout))
        },
        _ => {
          // Unused
        },
        true
      ))
      Thread.sleep(500)
    }
  }
}

@State(Scope.Thread)
@Threads(Threads.MAX)
class DeserializeToJsonBenchmark {
  import DeserializeToJsonBenchmark._

  @Benchmark
  @BenchmarkMode(Array(Mode.All))
  @Fork(1)
  @Measurement(iterations=12)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  @Warmup(iterations=10)
  def deserializeToJson(state: DataState, bh: Blackhole): Unit = {
    state.jsonData.lines().forEachOrdered(line => bh.consume(state.om.readTree(line)))
  }
}
