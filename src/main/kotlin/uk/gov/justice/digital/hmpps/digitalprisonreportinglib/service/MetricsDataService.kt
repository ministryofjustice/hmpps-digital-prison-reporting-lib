package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SchemaField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken

@Service
class MetricsDataService(
  val productDefinitionRepository: ProductDefinitionRepository,
  val configuredApiRepository: ConfiguredApiRepository,
) :
  CommonDataApiService() {

  fun validateAndFetchData(
    dataProductDefinitionId: String,
    metricId: String,
    userToken: DprAuthAwareAuthenticationToken?,
    dataProductDefinitionsPath: String? = null,
  ): List<Map<String, Any?>> {
    val metricProductDefinition = productDefinitionRepository.getSingleMetricProductDefinition(dataProductDefinitionId, metricId, dataProductDefinitionsPath)
    val policyEngine = PolicyEngine(metricProductDefinition.policy, userToken)
    return configuredApiRepository
      .executeQuery(
        query = metricProductDefinition.metricDataset.query,
        filters = emptyList(),
        selectedPage = 1,
        pageSize = 100,
        sortColumn = null,
        sortedAsc = true,
        policyEngineResult = policyEngine.execute(),
        dataSourceName = metricProductDefinition.datasource.name,
      )
      .let { records ->
        records.map {
          formatColumnNamesToSourceFieldNamesCasing(
            it,
            metricProductDefinition.metricDataset.schema.field.map(SchemaField::name),
          )
        }
      }
  }
}
