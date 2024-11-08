package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.DefinitionGsonConfig
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ChartDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ChartTypeDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DataDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.MetricDefinition
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
      dataProductDefinitionId = "missing-ethnicity-metrics",
      dashboardId = "test-dashboard-1",
    )
    assertEquals(
      DashboardDefinition(
        id = "test-dashboard-1",
        name = "Test Dashboard 1",
        description = "Test Dashboard 1 Description",
        metrics = listOf(
          MetricDefinition(
            id = "missing-ethnicity-metric",
            name = "Missing Ethnicity By Establishment Metric",
            display = "Missing Ethnicity By Establishment Metric",
            description = "Missing Ethnicity By Establishment Metric",
            unit = "number",
            charts = listOf(
              ChartDefinition(
                listOf(ChartTypeDefinition.BAR),
                dimension = "establishment_id",
              ),
            ),
            data = listOf(
              listOf(
                DataDefinition(name = "ethnicity_is_missing", display = "No. of Prisoners without ethnicity"),
                DataDefinition(name = "has_ethnicity", display = "No. of Prisoners with ethnicity"),
              ),
            ),
          ),
        ),
      ),
      actual,
    )
  }

//  @Test
//  fun `getMetricDefinition returns the metric definition`() {
//    val actual = metricDefinitionService.getMetricDefinition(
//      dataProductDefinitionId = "missing-ethnicity-metrics",
//      metricId = "missing-ethnicity-metric",
//    )
//    assertEquals(
//      MetricDefinition(
//        id = "missing-ethnicity-metric",
//        name = "testMetricId1",
//        display = "Missing Ethnicity",
//        description = "Missing Ethnicity",
//        specification = listOf(
//          MetricSpecificationDefinition(
//            name = "establishment_id",
//            display = "Establishment ID",
//            groupIndex = true,
//          ),
//          MetricSpecificationDefinition(
//            name = "missing_ethnicity_percentage",
//            display = "% Missing Ethnicity",
//            chart = listOf(ChartTypeDefinition.DOUGHNUT),
//            unit = "percentage",
//          ),
//          MetricSpecificationDefinition(
//            name = "present_ethnicity_percentage",
//            display = "% With Ethnicity",
//            chart = listOf(ChartTypeDefinition.DOUGHNUT),
//            unit = "percentage",
//          ),
//          MetricSpecificationDefinition(
//            name = "no_of_prisoners",
//            display = "No. of Prisoners with ethnicity",
//            chart = listOf(ChartTypeDefinition.BAR),
//          ),
//          MetricSpecificationDefinition(
//            name = "no_of_prisoners_without",
//            display = "No. of Prisoners without ethnicity",
//            chart = listOf(ChartTypeDefinition.BAR),
//          ),
//          MetricSpecificationDefinition(
//            name = "random_data",
//            display = "Random Data",
//          ),
//        ),
//      ),
//      actual,
//    )
//  }
}
