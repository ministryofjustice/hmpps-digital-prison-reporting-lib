package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.gson.JsonParseException
import org.slf4j.LoggerFactory
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.deser.std.StdDeserializer
import java.text.SimpleDateFormat
import java.util.Date

class DateDeserializer : StdDeserializer<Date>(Date::class.java) {
  private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Throws(JsonParseException::class)
  override fun deserialize(
    p: JsonParser?,
    ctxt: DeserializationContext?,
  ): Date? {
    val dateStr = p?.text ?: return null
    return try {
      formatter.parse(dateStr)
    } catch (e: Exception) {
      log.warn("Invalid date format: {}", dateStr, e)
      throw IllegalArgumentException("Unknown DateType $dateStr!")
    }
  }
}
