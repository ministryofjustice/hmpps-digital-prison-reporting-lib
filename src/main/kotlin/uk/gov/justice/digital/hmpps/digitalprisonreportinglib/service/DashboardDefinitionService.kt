package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ChartDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ChartTypeDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DataDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.MetricDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Chart
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ChartType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dashboard
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Data
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Metric
import java.lang.IllegalArgumentException

@Service
class DashboardDefinitionService(val productDefinitionRepository: ProductDefinitionRepository) {

  fun getDashboardDefinition(
    dataProductDefinitionId: String,
    dashboardId: String,
    dataProductDefinitionsPath: String? = null,
  ): DashboardDefinition {
    return toDashboardDefinition(
      productDefinitionRepository.getProductDefinition(
        dataProductDefinitionId,
        dataProductDefinitionsPath,
      ).dashboards?.firstOrNull { it.id == dashboardId }
        ?: throw IllegalArgumentException("Dashboard with ID: $dashboardId not found for DPD $dataProductDefinitionId"),
    )
  }

//  fun getMetricDefinition(
//    dataProductDefinitionId: String,
//    metricId: String,
//    dataProductDefinitionsPath: String? = null,
//  ): MetricDefinition {
//    return toMetricDefinition(
//      productDefinitionRepository.getProductDefinition(
//        dataProductDefinitionId,
//        dataProductDefinitionsPath,
//      ).metrics?.firstOrNull { it.id == metricId }
//        ?: throw IllegalArgumentException("Metric with ID: $metricId not found for DPD $dataProductDefinitionId"),
//    )
//  }

  fun toDashboardDefinition(dashboard: Dashboard): DashboardDefinition {
    return DashboardDefinition(
      id = dashboard.id,
      name = dashboard.name,
      description = dashboard.description,
      metrics = dashboard.metrics.map { toMetricDefinition(it) },
    )
  }

  private fun toMetricDefinition(metric: Metric): MetricDefinition {
    return MetricDefinition(
      id = metric.id,
      name = metric.name,
      display = metric.display,
      description = metric.description,
      charts = metric.charts.map { toChartDefinition(it) },
      data = metric.data.map { toDataDefinition(it) },
    )
  }

  private fun toChartDefinition(chart: Chart): ChartDefinition {
    return ChartDefinition(type = ChartTypeDefinition.valueOf(chart.type.toString()), dimension = chart.dimension)
  }

  private fun toDataDefinition(data: List<Data>): List<DataDefinition> {
    return data.map { DataDefinition(it.name.removePrefix("\$ref:"), it.display, it.unit) }
  }

  private fun toChartTypeDefinition(chart: List<ChartType>?): List<ChartTypeDefinition>? {
    return chart?.map { ChartTypeDefinition.valueOf(it.toString()) }
  }
}
