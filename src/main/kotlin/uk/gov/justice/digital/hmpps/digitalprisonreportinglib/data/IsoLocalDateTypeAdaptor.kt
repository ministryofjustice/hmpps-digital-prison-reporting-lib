package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class IsoLocalDateTypeAdaptor : LocalDateTypeAdaptor {
  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  override fun serialize(
    date: LocalDate?,
    typeOfSrc: Type?,
    context: JsonSerializationContext?,
  ): JsonElement {
    return JsonPrimitive(date?.format(formatter))
  }

  @Throws(JsonParseException::class)
  override fun deserialize(
    json: JsonElement,
    typeOfT: Type?,
    context: JsonDeserializationContext?,
  ): LocalDate {
    return LocalDate.parse(json.asString, formatter)
  }
}
