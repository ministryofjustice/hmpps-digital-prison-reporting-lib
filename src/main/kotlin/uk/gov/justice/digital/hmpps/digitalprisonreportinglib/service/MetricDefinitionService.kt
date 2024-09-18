package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ChartTypeDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.MetricDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.MetricSpecificationDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dashboard
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ChartType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Metric
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.MetricSpecification
import java.lang.IllegalArgumentException

@Service
class MetricDefinitionService(val productDefinitionRepository: ProductDefinitionRepository) {

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

  fun getMetricDefinition(
    dataProductDefinitionId: String,
    metricId: String,
    dataProductDefinitionsPath: String? = null,
  ): MetricDefinition {
    return toMetricDefinition(
      productDefinitionRepository.getProductDefinition(
        dataProductDefinitionId,
        dataProductDefinitionsPath,
      ).metrics?.firstOrNull { it.id == metricId }
        ?: throw IllegalArgumentException("Metric with ID: $metricId not found for DPD $dataProductDefinitionId"),
    )
  }

  fun toDashboardDefinition(dashboard: Dashboard): DashboardDefinition {
    return DashboardDefinition(
      id = dashboard.id,
      name = dashboard.name,
      description = dashboard.description,
      metrics = dashboard.metrics.map { toDashboardMetricDefinition(it) },
    )
  }

  private fun toMetricDefinition(metric: Metric): MetricDefinition {
    return MetricDefinition(
      id = metric.id,
      name = metric.name,
      display = metric.display,
      description = metric.description,
      specification = metric.specification.map { toMetricSpecificationDefinition(it) },
    )
  }

  private fun toMetricSpecificationDefinition(metricSpecification: MetricSpecification): MetricSpecificationDefinition {
    return MetricSpecificationDefinition(
      name = metricSpecification.name,
      display = metricSpecification.display,
      unit = metricSpecification.unit,
      chart = toChartTypeDefinition(metricSpecification.chart),
      group = metricSpecification.group,
    )
  }

  private fun toDashboardMetricDefinition(metric: Dashboard.DashboardMetric): DashboardDefinition.DashboardMetricDefinition {
    return DashboardDefinition.DashboardMetricDefinition(metric.id)
  }

  private fun toChartTypeDefinition(chart: List<ChartType>?): List<ChartTypeDefinition>? {
    return chart?.map { ChartTypeDefinition.valueOf(it.toString()) }
  }
}
