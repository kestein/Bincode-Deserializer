# Bincode-Deserializer
A deserializer for the bincode (https://github.com/TyOverby/bincode) format written in Scala

## Motivations
Creating a native Scala deserializer allows for ingest of bincode formatted files into a spark based system. As fast as Rust's serde is, the cost of converting bincode data to json then loading the json data is expensive. Making a native deserializer cuts out a large data conversion step when ingesting data.

## Usage
If you are familiar with Rust's serde framework this will seem familiar. Common primitive functions have been ported using the equivalent JVM types. Unsigned types have been modified to use the next byte order up since the JVM only handles signed numbers. For more complicated formats (maps, structs, enums) appropriate configuration classes exist. Define the deserialization logic in terms of the less complex types that make up the complex type. You can find some example structs and deserialization logic in the [various test classes](https://github.com/kestein/Bincode-Deserializer/blob/master/deserializer/src/test/scala/RandomStuff.scala#L27).
