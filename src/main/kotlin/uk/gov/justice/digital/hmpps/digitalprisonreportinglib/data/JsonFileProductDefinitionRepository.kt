package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinitionSummary
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.SyncDataApiService.Companion.INVALID_REPORT_ID_MESSAGE

class JsonFileProductDefinitionRepository(
  private val resourceLocations: List<String>,
  private val gson: Gson,
  identifiedHelper: IdentifiedHelper,
) : AbstractProductDefinitionRepository(identifiedHelper) {

  override fun getProductDefinitions(path: String?): List<ProductDefinitionSummary> = resourceLocations.map { gson.fromJson(this::class.java.classLoader.getResource(it)?.readText(), object : TypeToken<ProductDefinitionSummary>() {}.type) }

  override fun getProductDefinition(definitionId: String, dataProductDefinitionsPath: String?): ProductDefinition = doGetProductDefinitions(dataProductDefinitionsPath)
    .filter { it.id == definitionId }
    .ifEmpty { throw ValidationException("$INVALID_REPORT_ID_MESSAGE $definitionId") }
    .first()

  fun doGetProductDefinitions(path: String?): List<ProductDefinition> = resourceLocations.map { gson.fromJson(this::class.java.classLoader.getResource(it)?.readText(), object : TypeToken<ProductDefinition>() {}.type) }

}
