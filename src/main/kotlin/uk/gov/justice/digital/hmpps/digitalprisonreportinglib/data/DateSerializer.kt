package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ser.std.StdSerializer
import java.text.SimpleDateFormat
import java.util.Date

class DateSerializer : StdSerializer<Date>(Date::class.java) {

  private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  override fun serialize(value: Date, gen: JsonGenerator, ctxt: SerializationContext) {
    val formattedDate = formatter.format(value)
    gen.writeString(formattedDate)
  }
}
