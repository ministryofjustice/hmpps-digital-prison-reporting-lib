package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.DefinitionGsonConfig
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardChartTypeDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardDefinition.DashboardMetricDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.MetricDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.MetricSpecificationDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.IsoLocalDateTimeTypeAdaptor
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.JsonFileProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository

class MetricDefinitionServiceTest {

  private val productDefinitionRepository: ProductDefinitionRepository = JsonFileProductDefinitionRepository(
    listOf("productDefinitionWithMetrics.json"),
    DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
  )

  private val metricDefinitionService = MetricDefinitionService(productDefinitionRepository)

  @Test
  fun `getDashboardDefinition returns the dashboard definition`() {
    val actual = metricDefinitionService.getDashboardDefinition(
      dataProductDefinitionId = "external-movements",
      dashboardId = "test-dashboard-1",
    )
    assertEquals(
      DashboardDefinition(
        id = "test-dashboard-1",
        name = "Test Dashboard 1",
        description = "Test Dashboard 1 Description",
        metrics = listOf(
          DashboardMetricDefinition(id = "test-metric-id-1", listOf(DashboardChartTypeDefinition.BAR)),
        ),
      ),
      actual,
    )
  }

  @Test
  fun `getMetricDefinition returns the metric definition`() {
    val actual = metricDefinitionService.getMetricDefinition(
      dataProductDefinitionId = "external-movements",
      metricId = "test-metric-id-1",
    )
    assertEquals(
      MetricDefinition(
        id = "test-metric-id-1",
        name = "testMetricId1",
        display = "Prisoner Images by Status Percentage",
        description = "Prisoner Images by Status Percentage",
        visualisationType = listOf(
          DashboardChartTypeDefinition.BAR,
          DashboardChartTypeDefinition.DOUGHNUT,
        ),
        specification = listOf(
          MetricSpecificationDefinition(
            name = "status",
            display = "Status",
          ),
          MetricSpecificationDefinition(
            name = "count",
            display = "Count",
            unit = "percentage",
          ),
        ),
      ),
      actual,
    )
  }
}
