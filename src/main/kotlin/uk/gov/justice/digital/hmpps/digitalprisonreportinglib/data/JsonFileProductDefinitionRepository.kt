package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ParameterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import java.time.LocalDateTime

class JsonFileProductDefinitionRepository(
  private val localDateTimeTypeAdaptor: LocalDateTimeTypeAdaptor,
  private val resourceLocations: List<String>,
  private val filterTypeDeserializer: FilterTypeDeserializer,
  private val schemaFieldTypeDeserializer: SchemaFieldTypeDeserializer,
) : AbstractProductDefinitionRepository() {

  override fun getProductDefinitions(): List<ProductDefinition> {
    val gson: Gson = GsonBuilder()
      .registerTypeAdapter(LocalDateTime::class.java, localDateTimeTypeAdaptor)
      .registerTypeAdapter(FilterType::class.java, filterTypeDeserializer)
      .registerTypeAdapter(ParameterType::class.java, schemaFieldTypeDeserializer)
      .create()
    return resourceLocations.map { gson.fromJson(this::class.java.classLoader.getResource(it)?.readText(), object : TypeToken<ProductDefinition>() {}.type) }
  }

  override fun getProductDefinition(definitionId: String): ProductDefinition = getProductDefinitions()
    .filter { it.id == definitionId }
    .ifEmpty { throw ValidationException("Invalid report id provided: $definitionId") }
    .first()
}
