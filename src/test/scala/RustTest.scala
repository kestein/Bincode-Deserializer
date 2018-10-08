import java.io.{File, FileInputStream, PipedInputStream, PipedOutputStream}
import java.nio.file.Files.createTempDirectory

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.scalatest.FunSuite

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.sys.process._

class RustTest extends FunSuite {
  test("validateOne") {
    // Create the rust file
    val tempDirectory = createTempDirectory("scalaTest").toString
    val rustBasename: String = setupRustFiles(tempDirectory, "validateOne", 1)
    // Deserialize from rust to scala
    val d = new Deserializer(new FileInputStream(new StringBuilder(rustBasename).append(".bin").result()))
    val deserializedValue: RandomStuff = d.deserialize_struct[RandomStuff](new RandomStuffFactory).fold(err => throw new DeserializerException(err), rs => rs)
    // Write out what scala deserialized
    val scalaFilename = setupDeserializedFile(tempDirectory, "validateOne", deserializedValue)
    // Validate
    val in = new PipedInputStream()
    val stdout = new PipedOutputStream(in)
    Future {
      val rustFilename = new StringBuilder(rustBasename).append(".json").result()
      (s"src\\test\\rust\\bincode-tester\\target\\release\\bincode-tester.exe validate -b $rustFilename -a $scalaFilename" #> stdout).!
    }
    val validationOutput = new String(in.readAllBytes())
    assert(validationOutput.length == 0)
  }
  test("differentSeeds") {
    // Create the rust file
    val tempDirectory = createTempDirectory("scalaTest").toString
    val rustBasename: String = setupRustFiles(tempDirectory, "differentSeeds", 1)
    // Create the wrong file to compare against
    val invalidRustFilename: String = setupRustFiles(tempDirectory, "differentSeeds-invalid", 1, "Saratoga").concat(".json")
    // Deserialize from rust to scala using rustBasename
    val d = new Deserializer(new FileInputStream(new StringBuilder(rustBasename).append(".bin").result()))
    val deserializedValue: RandomStuff = d.deserialize_struct[RandomStuff](new RandomStuffFactory).fold(err => throw new DeserializerException(err), rs => rs)
    // Write out what scala deserialized
    val scalaFilename: String = setupDeserializedFile(tempDirectory, "differentSeeds", deserializedValue)
    // Validate
    val in = new PipedInputStream()
    val stdout = new PipedOutputStream(in)
    Future {
      (s"src\\test\\rust\\bincode-tester\\target\\release\\bincode-tester.exe validate -b $invalidRustFilename -a $scalaFilename" #> stdout).!
    }
    val validationOutput = new String(in.readAllBytes())
    assert(validationOutput.length > 0)
    println(validationOutput)
  }
  // Multiple values

  /*
    Creates the necessary files to deserialize data into scala and validate scala to rust

    @param dir: The directory to write the files to
    @param suffix: The suffix to add to the filename. Should not include the extension.
    @param amount: The number of records the test generator will generate
    @param seed: The seed to run the generator with
    @return: The base name for the json and bincode rust files. Includes the directory.
   */
  def setupRustFiles(dir: String, suffix: String, amount: Int, seed: String="USS_Hornet"): String = {
    val rustBasename = new StringBuilder(dir).append("rust-").append(suffix).result()
    // Write the bincode file
    val binName = new StringBuilder(rustBasename).append(".bin").result()
    s"src\\test\\rust\\bincode-tester\\target\\release\\bincode-tester.exe -s $seed -o $binName write -a $amount".!
    // Write the json file to validate against
    val jsonName = new StringBuilder(rustBasename).append(".json").result()
    (s"src\\test\\rust\\bincode-tester\\target\\release\\bincode-tester.exe -s $seed write -a $amount" #|
      s"src\\test\\rust\\bincode-tester\\target\\release\\bincode-tester.exe -o $jsonName convert ").!
    rustBasename
  }

  /*
    Creates the json file representing the data that was deserialized
   */
  def setupDeserializedFile(dir: String, suffix: String, deserializedValue: RandomStuff): String = {
    val scalaFilename = new StringBuilder(dir).append("scala-").append(suffix).append(".json").result()
    val om = new ObjectMapper()
    om.registerModule(DefaultScalaModule)
    om.writeValue(new File(scalaFilename), deserializedValue)
    scalaFilename
  }
}
