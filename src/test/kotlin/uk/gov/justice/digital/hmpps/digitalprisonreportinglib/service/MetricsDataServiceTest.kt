package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.security.core.GrantedAuthority
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.DefinitionGsonConfig
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.IsoLocalDateTimeTypeAdaptor
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.JsonFileProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken

class MetricsDataServiceTest {

  private val productDefinitionRepository: ProductDefinitionRepository = JsonFileProductDefinitionRepository(
    listOf("productDefinitionWithMetrics.json"),
    DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
  )
  private val configuredApiRepository = mock<ConfiguredApiRepository>()
  private val authToken = mock<DprAuthAwareAuthenticationToken>()

  @BeforeEach
  fun setup() {
    whenever(authToken.getCaseLoads()).thenReturn(listOf("WWI"))
    whenever(authToken.authorities).thenReturn(listOf(GrantedAuthority { "ROLE_PRISONS_REPORTING_USER" }))
  }

  @Test
  fun `validateAndFetchData should call the configuredApiRepository to retrieve the data`() {
    val dataProductDefinitionId = "missing-ethnicity-metrics"
    val metricId = "missing-ethnicity-metric"
    val policyEngineResult = "(establishment_id='WWI')"
    val singleMetricDefinition =
      productDefinitionRepository.getSingleMetricProductDefinition(dataProductDefinitionId, metricId)
    val metricsDataService = MetricsDataService(productDefinitionRepository, configuredApiRepository)

    val expectedServiceResult = listOf(
      mapOf(
        "establishment_id" to "WWI",
        "missing_ethnicity_percentage" to 2,
        "present_ethnicity_percentage" to 98,
        "no_of_prisoners" to 196,
        "no_of_prisoners_without" to 4,
        "random_data" to 20,
      ),
    )
    whenever(
      configuredApiRepository.executeQuery(
        query = singleMetricDefinition.metricDataset.query,
        filters = emptyList(),
        selectedPage = 1,
        pageSize = 100,
        sortColumn = null,
        sortedAsc = true,
        policyEngineResult = policyEngineResult,
        dataSourceName = singleMetricDefinition.datasource.name,
      ),
    ).thenReturn(expectedServiceResult)

    val actual = metricsDataService.validateAndFetchData(
      dataProductDefinitionId,
      metricId,
      authToken,
    )

    assertEquals(expectedServiceResult, actual)
  }
}
