import java.io._
import java.nio.file.Files.createTempDirectory

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.scalatest.FunSuite

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.sys.process._

class RustTest extends FunSuite {
  val bincodeTesterPath: String = RustTest.bincodeTesterPath
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
      (s"$bincodeTesterPath validate -b $rustFilename -a $scalaFilename" #> stdout).!
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
      (s"$bincodeTesterPath validate -b $invalidRustFilename -a $scalaFilename" #> stdout).!
    }
    val validationOutput = new String(in.readAllBytes())
    assert(validationOutput.length > 0)
    println(validationOutput)
  }
  test("validateMany") {
    // Create the rust file
    val tempDirectory = createTempDirectory("scalaTest").toString
    val rustBasename: String = setupRustFiles(tempDirectory, "validateMany", 10)
    // Deserialize from rust to scala
    val d = new Deserializer(new FileInputStream(new StringBuilder(rustBasename).append(".bin").result()))
    var deserializedValues: Seq[RandomStuff] = Seq()
    var deserializedValue: Deserializer.DeserializeResult[RandomStuff] = null
    while({
      deserializedValue = d.deserialize_struct[RandomStuff](new RandomStuffFactory)
      deserializedValue.isRight
    }) {
      deserializedValues = deserializedValues :+ deserializedValue.right.get
    }
    // Write out what scala deserialized
    val scalaFilename = setupDeserializedFile(tempDirectory, "validateMany", deserializedValues)
    // Validate
    val in = new PipedInputStream()
    val stdout = new PipedOutputStream(in)
    Future {
      val rustFilename = new StringBuilder(rustBasename).append(".json").result()
      (s"$bincodeTesterPath validate -b $rustFilename -a $scalaFilename" #> stdout).!
    }
    val validationOutput = new String(in.readAllBytes())
    assert(validationOutput.length == 0)
  }

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
    s"$bincodeTesterPath -s $seed -o $binName write -a $amount".!
    // Write the json file to validate against
    val jsonName = new StringBuilder(rustBasename).append(".json").result()
    (s"$bincodeTesterPath -s $seed write -a $amount" #|
      s"$bincodeTesterPath -o $jsonName convert ").!
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

  def setupDeserializedFile(dir: String, suffix: String, deserializedValues: Seq[RandomStuff]): String = {
    val scalaFilename = new StringBuilder(dir).append("scala-").append(suffix).append(".json").result()
    val om = new ObjectMapper()
    val scalaFile = new BufferedWriter(new FileWriter(scalaFilename))
    om.registerModule(DefaultScalaModule)
    deserializedValues.foreach(r => {
      scalaFile.write(om.writeValueAsString(r))
      scalaFile.newLine()
    })
    scalaFile.close()
    scalaFilename
  }
}

object RustTest {
  val bincodeTesterPath: String = "deserializer\\src\\test\\rust\\bincode-tester\\target\\release\\bincode-tester.exe"
}
