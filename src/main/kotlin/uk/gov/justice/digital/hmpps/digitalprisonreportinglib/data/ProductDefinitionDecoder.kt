package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.gson.Gson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinitionSummary
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.UnsupportedVersionException

class ProductDefinitionDecoder(
  private val dprDefinitionGson: Gson,
  private val dprDefinitionKotlinx: Json,
) {

  companion object {
    const val SCHEMA_VERSION_FIELD = "schemaversion"
    const val V_1_0_0 = "1.0.0"
    const val V_1_1_0 = "1.1.0"
  }

  fun decode(json: String): DecodedProductDefinition {
    val root = parseProductDefinition(json)

    return when (val schemaVersion = extractSchemaVersion(root)) {
      null,
      V_1_0_0,
      -> decodeLenientlyWithGson(json)

      V_1_1_0 -> decodeStrictlyWithKotlinx(root)

      else -> throw UnsupportedVersionException("Unsupported schemaversion '$schemaVersion'. Supported versions are $V_1_0_0 and $V_1_1_0.")
    }
  }

  private fun parseProductDefinition(json: String): JsonObject = dprDefinitionKotlinx.parseToJsonElement(json) as? JsonObject ?: throw InvalidProductDefinitionException("The DPD must be a JSON object")

  private fun extractSchemaVersion(root: JsonObject): String? {
    return extractVersionString(root[SCHEMA_VERSION_FIELD] ?: return null)
  }

  private fun extractVersionString(versionElement: JsonElement): String {
    val versionPrimitive = versionElement as? JsonPrimitive

    if (versionPrimitive == null || !versionPrimitive.isString) {
      throw InvalidProductDefinitionException("$SCHEMA_VERSION_FIELD must be a JSON string")
    }

    return versionPrimitive.content
  }

  private fun decodeStrictlyWithKotlinx(root: JsonObject): DecodedProductDefinition {
    val definition = dprDefinitionKotlinx.decodeFromJsonElement<ProductDefinition>(root)

    return DecodedProductDefinition(
      definition = definition,
      summary = definition.toSummary(),
    )
  }

  private fun decodeLenientlyWithGson(json: String): DecodedProductDefinition = DecodedProductDefinition(
    definition = dprDefinitionGson.fromJson(json, ProductDefinition::class.java),
    summary = dprDefinitionGson.fromJson(json, ProductDefinitionSummary::class.java),
  )
}

data class DecodedProductDefinition(
  val summary: ProductDefinitionSummary,
  val definition: ProductDefinition,
)

class InvalidProductDefinitionException(message: String) : IllegalArgumentException(message)
