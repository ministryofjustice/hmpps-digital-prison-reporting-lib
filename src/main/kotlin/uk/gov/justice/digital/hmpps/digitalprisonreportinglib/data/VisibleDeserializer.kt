package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Visible
import java.lang.reflect.Type

class VisibleDeserializer : JsonDeserializer<Visible?> {
  @Throws(JsonParseException::class)
  override fun deserialize(
    json: JsonElement,
    typeOfT: Type?,
    context: JsonDeserializationContext?,
  ): Visible {
    val stringValue = json.asString
    return Visible.entries.firstOrNull { enum -> enum.toString().lowercase() == stringValue }
      ?: throw IllegalArgumentException("Unknown Visible value $stringValue!")
  }
}
