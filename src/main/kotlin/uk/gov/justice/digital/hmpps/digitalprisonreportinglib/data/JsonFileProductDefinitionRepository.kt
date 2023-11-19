package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import java.lang.reflect.Type
import java.time.LocalDate

class JsonFileProductDefinitionRepository(
  private val localDateTypeAdaptor: LocalDateTypeAdaptor,
  private val resourceLocation: String,
) : ProductDefinitionRepository {

  companion object {
    private const val schemaRefPrefix = "\$ref:"
  }

  override fun getProductDefinitions(): List<ProductDefinition> {
    val gson: Gson = GsonBuilder()
      .registerTypeAdapter(LocalDate::class.java, localDateTypeAdaptor)
      .registerTypeAdapter(FilterType::class.java, FilterTypeDeserializer())
      .create()
    return listOf(gson.fromJson(this::class.java.classLoader.getResource(resourceLocation)?.readText(), object : TypeToken<ProductDefinition>() {}.type))
  }

  override fun getProductDefinition(definitionId: String): ProductDefinition = getProductDefinitions()
    .filter { it.id == definitionId }
    .ifEmpty { throw ValidationException("Invalid report id provided: $definitionId") }
    .first()

  override fun getSingleReportProductDefinition(definitionId: String, reportId: String): SingleReportProductDefinition {
    val productDefinition = getProductDefinition(definitionId)
    val reportDefinition = productDefinition.report
      .filter { it.id == reportId }
      .ifEmpty { throw ValidationException("Invalid report variant id provided: $reportId") }
      .first()

    val dataSetId = reportDefinition.dataset.removePrefix(schemaRefPrefix)
    val dataSet = productDefinition.dataset
      .filter { it.id == dataSetId }
      .ifEmpty { throw ValidationException("Invalid dataSetId in report: $dataSetId") }
      .first()

    return SingleReportProductDefinition(
      id = definitionId,
      name = productDefinition.name,
      description = productDefinition.description,
      metadata = productDefinition.metadata,
      dataSource = productDefinition.dataSource.first(),
      dataset = dataSet,
      report = reportDefinition,
    )
  }
}
class FilterTypeDeserializer : com.google.gson.JsonDeserializer<FilterType?> {

  @Throws(JsonParseException::class)
  override fun deserialize(
    json: JsonElement,
    typeOfT: Type?,
    context: JsonDeserializationContext?,
  ): FilterType {
    val stringValue = json?.asString
    for (enum in FilterType.values()) {
      if (enum.type == stringValue) {
        return enum
      }
    }
    throw IllegalArgumentException("Unknown tsp $stringValue!")
  }
}
