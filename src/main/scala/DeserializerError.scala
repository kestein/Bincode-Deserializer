class DeserializerError(val message: String)

class DeserializerException(message: String, reason: Exception) extends Exception(message, reason) {
  def this(message: String) = {
    this(message, null)
  }

  def this(error: DeserializerError) = {
    this(error.message, null)
  }
}

class InvalidVariantError(message: String) extends DeserializerError(message) {
  def this(variant: Int, expected: Seq[String]) = {
    this(s"$variant is not a valid variant. Expected ".concat(expected.mkString(",")))
  }
}

class DeserializeIOError(message: String) extends DeserializerError(message) {
  def this(requested: Int, got: Int) = {
    this(s"Unable to read the request number of bytes. Requested $requested git $got")
  }
}