package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.MultiphaseQuery
import java.lang.reflect.Type

class QueryDeserializer :
  JsonDeserializer<List<MultiphaseQuery>>,
  KSerializer<List<MultiphaseQuery>> {

  companion object {
    // This is not used in single element MultiphaseQuery lists. It is here for compatibility with multiple query element execution as the datasource is required and used in MultiphaseQuery in this case.
    const val PLACEHOLDER_DATASOURCE = "PLACEHOLDER_DATASOURCE"
  }

  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset.Query", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): List<MultiphaseQuery> = when (val element = (decoder as JsonDecoder).decodeJsonElement()) {
    is JsonArray -> decoder.json.decodeFromJsonElement(ListSerializer(MultiphaseQuery.serializer()), element)
    is JsonPrimitive -> {
      require(element.isString) { "Expected string or array but got $element" }
      listOf(
        MultiphaseQuery(
          index = 0,
          datasource = PLACEHOLDER_DATASOURCE,
          query = element.content,
        ),
      )
    }
    else -> throw SerializationException("Unexpected element type ${element.jsonObject}")
  }

  override fun serialize(encoder: Encoder, value: List<MultiphaseQuery>) = encoder.encodeSerializableValue(ListSerializer(MultiphaseQuery.serializer()), value)

  override fun deserialize(
    json: JsonElement,
    typeOfT: Type,
    context: JsonDeserializationContext,
  ): List<MultiphaseQuery> = when {
    json.isJsonPrimitive && json.asJsonPrimitive.isString -> {
      listOf(
        MultiphaseQuery(
          index = 0,
          datasource = PLACEHOLDER_DATASOURCE,
          query = json.asString,
        ),
      )
    }
    json.isJsonArray -> {
      json.asJsonArray.map { element ->
        context.deserialize<MultiphaseQuery>(
          element,
          MultiphaseQuery::class.java,
        )
      }
    }
    else -> {
      throw JsonParseException(
        "Invalid 'query' field. Expected string or array of MultiphaseQuery.",
      )
    }
  }
}
