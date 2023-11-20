package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterType
import java.lang.reflect.Type

class FilterTypeDeserializer : JsonDeserializer<FilterType?> {

  @Throws(JsonParseException::class)
  override fun deserialize(
    json: JsonElement,
    typeOfT: Type?,
    context: JsonDeserializationContext?,
  ): FilterType {
    val stringValue = json.asString
    return FilterType.entries.firstOrNull { enum -> enum.type == stringValue }
      ?: throw IllegalArgumentException("Unknown FilterType $stringValue!")
  }
}
