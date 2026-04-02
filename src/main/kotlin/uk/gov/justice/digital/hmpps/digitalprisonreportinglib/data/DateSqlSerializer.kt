package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ser.std.StdSerializer
import java.sql.Date
import java.text.SimpleDateFormat

class DateSqlSerializer : StdSerializer<Date>(Date::class.java) {
  private val formatter = SimpleDateFormat("yyyy-MM-dd")

  override fun serialize(
    value: Date?,
    gen: JsonGenerator?,
    ctxt: SerializationContext?,
  ) {
    val formattedDate = formatter.format(value)
    gen?.writeString(formattedDate)
  }
}
