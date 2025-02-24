package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class IsoLocalDateTimeTypeAdaptor : LocalDateTimeTypeAdaptor {
  override fun serialize(
    date: LocalDateTime?,
    typeOfSrc: Type?,
    context: JsonSerializationContext?,
  ): JsonElement = JsonPrimitive(date?.format(DateTimeFormatter.ISO_DATE_TIME))

  @Throws(JsonParseException::class)
  override fun deserialize(
    json: JsonElement,
    typeOfT: Type?,
    context: JsonDeserializationContext?,
  ): LocalDateTime = LocalDateTime.parse(json.asString, DateTimeFormatter.ISO_DATE_TIME)
}
