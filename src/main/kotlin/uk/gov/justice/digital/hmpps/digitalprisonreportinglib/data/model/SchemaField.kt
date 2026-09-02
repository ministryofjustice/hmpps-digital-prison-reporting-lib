package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonObject

@Serializable(with = LegacySchemaFieldSerializerDeserializer::class)
data class SchemaField(
  val name: String,
  val type: ParameterType,
  val display: String,
  val filter: FilterDefinition? = null,
  val formula: String? = null,
) : Identified {
  override fun getIdentifier() = this.name
}

/**
 * Kotlinx serializer/deserializer so that we can leave it exactly as-is whilst also allowing kotlinx.serialization lib to work properly.
 */
class LegacySchemaFieldSerializerDeserializer : KSerializer<SchemaField> {
  override val descriptor = PrimitiveSerialDescriptor("uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Specification.section", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): SchemaField {
    decoder as JsonDecoder

    val el = decoder.decodeJsonElement()
    val name = decoder.json.decodeFromJsonElement(String.serializer(), el.jsonObject["name"]!!)
    val type = decoder.json.decodeFromJsonElement(ParameterType.serializer(), el.jsonObject["type"]!!)
    val display = if (el.jsonObject["display"] != null) decoder.json.decodeFromJsonElement(String.serializer(), el.jsonObject["display"]!!) else null
    val filter = if (el.jsonObject["filter"] != null) decoder.json.decodeFromJsonElement(FilterDefinition.serializer(), el.jsonObject["filter"]!!) else null
    val formula = if (el.jsonObject["formula"] != null) decoder.json.decodeFromJsonElement(String.serializer(), el.jsonObject["formula"]!!) else null

    return SchemaField(
      name,
      type,
      display ?: String(),
      filter,
      formula,
    )
  }

  override fun serialize(encoder: Encoder, value: SchemaField) {
    encoder.encodeSerializableValue(SchemaField.serializer(), value)
  }
}
