package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ser.std.StdSerializer
import java.util.Date

class DateSerializer : StdSerializer<Date>(Date::class.java) {
  override fun serialize(value: Date, gen: JsonGenerator, ctxt: SerializationContext) {
    gen.writeNumber(value.time) // milliseconds since epoch
  }
}
