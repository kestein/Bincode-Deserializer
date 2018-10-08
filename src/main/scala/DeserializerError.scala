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
    this(new StringBuilder(variant).append(" is not a valid variant. Expected ").append(expected.mkString(",")).result())
  }
}
