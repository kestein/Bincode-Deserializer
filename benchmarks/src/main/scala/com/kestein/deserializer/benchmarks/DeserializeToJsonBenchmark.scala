package com.kestein.deserializer.benchmarks

import java.util.concurrent.TimeUnit

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.openjdk.jmh.annotations.Mode.AverageTime
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import scala.sys.process._

/* Benchmark of taking the data as a string and loading it as a JsonNode.
 * Does not measure the time it takes to convert the data from bincode to json.*/
@State(Scope.Thread)
class DeserializeToJsonBenchmark {
  @Param(Array("1", "10", "100"))
  var iterations: Int = 1
  var jsonIterator: Iterator[String] = _
  final val om = new ObjectMapper
  om.registerModule(DefaultScalaModule)

  @Setup(Level.Invocation)
  def generateJson(): Unit = {
    jsonIterator = (s"bincode-tester.exe write -a $iterations" #|
      s"bincode-tester.exe convert ").lineStream.iterator
  }

  @Benchmark
  @BenchmarkMode(Array(AverageTime))
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def deserializeToJson(bh: Blackhole): Unit = {
    jsonIterator.foreach(line => bh.consume(om.readTree(line)))
  }
}
