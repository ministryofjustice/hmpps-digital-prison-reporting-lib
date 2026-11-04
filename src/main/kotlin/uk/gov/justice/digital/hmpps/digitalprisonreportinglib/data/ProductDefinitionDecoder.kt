package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinitionSummary
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.UnsupportedVersionException
import kotlinx.serialization.json.Json

class ProductDefinitionDecoder(
  private val dprDefinitionGson: Gson,
  private val dprDefinitionKotlinx: Json) {

  companion object {
    const val SCHEMA_VERSION_FIELD = "schemaversion"
    const val V_1_1_0 = "1.1.0"
    const val V_1_0_0 = "1.0.0"
  }

  fun decode(json: String, ): DecodedProductDefinition {
    return when (val schemaVersion = extractSchemaVersion(json)) {
      null,
      V_1_0_0 -> decodeLeniently(json)

      V_1_1_0 -> decodeStrictly(json)

      else -> throw UnsupportedVersionException("Unsupported schemaversion: $schemaVersion. " +
        "Supported versions are: $V_1_0_0, $V_1_1_0.",)
    }
  }

  private fun extractSchemaVersion(json: String): String? {
    val root = JsonParser.parseString(json)

    if (!root.isJsonObject) {
      throw InvalidProductDefinitionException("The DPD must be a JSON object")
    }

    val jsonObject = root.asJsonObject

    val versionElement = jsonObject.get(SCHEMA_VERSION_FIELD) ?: return null

    return extractVersionString(versionElement)
  }

  private fun extractVersionString(versionElement: JsonElement): String {
    if (versionElement.isJsonNull ||
      !versionElement.isJsonPrimitive ||
      !versionElement.asJsonPrimitive.isString) {
      throw InvalidProductDefinitionException(
        "$SCHEMA_VERSION_FIELD must be a JSON string",
      )
    }

    return versionElement.asString
  }

  private fun decodeStrictly(json: String): DecodedProductDefinition {

    val definition = dprDefinitionKotlinx.decodeFromString<ProductDefinition>(json)

    return DecodedProductDefinition(
      definition = definition,
      summary = definition.toSummary(),
    )
  }

  private fun decodeLeniently(json: String, ): DecodedProductDefinition =
    DecodedProductDefinition(
      summary = dprDefinitionGson.fromJson(json, ProductDefinitionSummary::class.java,),
      definition = dprDefinitionGson.fromJson(json, ProductDefinition::class.java,),
    )
}

data class DecodedProductDefinition(
  val summary: ProductDefinitionSummary,
  val definition: ProductDefinition,
)

class InvalidProductDefinitionException(
  message: String,
) : IllegalArgumentException(message)