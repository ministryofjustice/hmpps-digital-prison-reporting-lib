package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.slf4j.LoggerFactory
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.deser.std.StdDeserializer
import java.sql.Date
import java.text.SimpleDateFormat

class DateSqlDeserializer : StdDeserializer<Date>(Date::class.java) {
  private val formatter = SimpleDateFormat("yyyy-MM-dd")

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

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
    } as Date?
  }
}
