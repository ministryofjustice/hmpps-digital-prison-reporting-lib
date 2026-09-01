package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonObject

@Serializable(with = LegacySpecificationSerializerDeserializer::class)
data class Specification(
  val template: Template,
  val field: List<ReportField>,
  val section: List<String>?,
)

/**
 * Kotlinx serializer/deserializer so that we can leave it exactly as-is whilst also allowing kotlinx.serialization lib to work properly.
 */
class LegacySpecificationSerializerDeserializer : KSerializer<Specification> {
  override val descriptor = PrimitiveSerialDescriptor("uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Specification.section", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): Specification {
    decoder as JsonDecoder

    val el = decoder.decodeJsonElement()
    val template = decoder.json.decodeFromJsonElement(Template.serializer(), el.jsonObject["template"]!!)
    val field = decoder.json.decodeFromJsonElement(ListSerializer(ReportField.serializer()), el.jsonObject["field"]!!)

    val sectionJson = el.jsonObject["section"]
    val section = if (sectionJson != null) decoder.json.decodeFromJsonElement(ListSerializer(String.serializer()), el.jsonObject["field"]!!) else null

    return Specification(template, field, section)
  }

  override fun serialize(encoder: Encoder, value: Specification) {
    encoder.encodeSerializableValue(Specification.serializer(), value)
  }
}
