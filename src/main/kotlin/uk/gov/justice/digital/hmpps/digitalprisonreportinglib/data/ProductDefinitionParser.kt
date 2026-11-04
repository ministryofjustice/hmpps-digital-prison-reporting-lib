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

class ProductDefinitionParser(
  private val dprDefinitionGson: Gson,
  private val dprDefinitionKotlinx: Json,
) {

  companion object {
    const val SCHEMA_VERSION_FIELD = "schemaversion"
    const val V_1_0_0 = "1.0.0"
    const val V_1_1_0 = "1.1.0"
  }

  fun parse(json: String): ParsedProductDefinition {
    val root = parseProductDefinition(json)

    return when (val schemaVersion = extractSchemaVersion(root)) {
      null,
      V_1_0_0,
      -> parseLenientlyWithGson(json)

      V_1_1_0 -> parseStrictlyWithKotlinx(root)

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

  private fun parseStrictlyWithKotlinx(root: JsonObject): ParsedProductDefinition {
    val definition = dprDefinitionKotlinx.decodeFromJsonElement<ProductDefinition>(root)

    return ParsedProductDefinition(
      definition = definition,
      summary = definition.toSummary(),
    )
  }

  private fun parseLenientlyWithGson(json: String): ParsedProductDefinition = ParsedProductDefinition(
    definition = dprDefinitionGson.fromJson(json, ProductDefinition::class.java),
    summary = dprDefinitionGson.fromJson(json, ProductDefinitionSummary::class.java),
  )
}

data class ParsedProductDefinition(
  val summary: ProductDefinitionSummary,
  val definition: ProductDefinition,
)

class InvalidProductDefinitionException(message: String) : IllegalArgumentException(message)
