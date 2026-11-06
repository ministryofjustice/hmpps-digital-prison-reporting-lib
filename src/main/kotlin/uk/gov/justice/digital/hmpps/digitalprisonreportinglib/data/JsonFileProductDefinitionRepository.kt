package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinitionSummary
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.SyncDataApiService.Companion.INVALID_REPORT_ID_MESSAGE

class JsonFileProductDefinitionRepository(
  private val resourceLocations: List<String>,
  private val productDefinitionParser: ProductDefinitionParser,
  identifiedHelper: IdentifiedHelper,
) : AbstractProductDefinitionRepository(identifiedHelper) {

  override fun getProductDefinitions(): List<ProductDefinitionSummary> = resourceLocations.map { readLocalDefinition(it).summary }

  override fun getProductDefinition(definitionId: String): ProductDefinition = doGetProductDefinitions()
    .filter { it.id == definitionId }
    .ifEmpty { throw ValidationException("$INVALID_REPORT_ID_MESSAGE $definitionId") }
    .first()

  fun doGetProductDefinitions(): List<ProductDefinition> = resourceLocations.map { readLocalDefinition(it).definition }

  private fun readLocalDefinition(location: String): ParsedProductDefinition {
    val resource = requireNotNull(javaClass.classLoader.getResource(location)) {
      "Product definition resource not found: $location"
    }
    return productDefinitionParser.parse(resource.readText())
  }
}
