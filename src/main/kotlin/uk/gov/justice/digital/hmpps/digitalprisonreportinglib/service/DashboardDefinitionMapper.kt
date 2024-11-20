package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ChartDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ChartTypeDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ColumnDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FieldDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FilterOption
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.LabelDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.MetricDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.DatasetHelper
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Chart
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Column
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dashboard
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Label
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Metric
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SchemaField

@Component
class DashboardDefinitionMapper(
  syncDataApiService: SyncDataApiService,
  datasetHelper: DatasetHelper,
) : DefinitionMapper(syncDataApiService, datasetHelper) {
  fun toDashboardDefinition(dashboard: Dashboard, allDatasets: List<Dataset>): DashboardDefinition {
    val dataset = datasetHelper.findDataset(allDatasets, dashboard.dataset)

    return DashboardDefinition(
      id = dashboard.id,
      name = dashboard.name,
      description = dashboard.description,
      metrics = dashboard.metrics.map { toMetricDefinition(it) },
      filterFields = dataset.schema.field
        .filter { it.filter != null }
        .map { toFilterField(it, allDatasets) },
    )
  }

  private fun toMetricDefinition(metric: Metric): MetricDefinition {
    return MetricDefinition(
      id = metric.id,
      name = metric.name,
      display = metric.display,
      description = metric.description,
      charts = metric.charts.map { toChartDefinition(it) },
    )
  }

  private fun toChartDefinition(chart: Chart): ChartDefinition {
    return ChartDefinition(
      type = ChartTypeDefinition.valueOf(chart.type.toString()),
      label = toLabelDefinition(chart.label),
      unit = chart.unit,
      columns = chart.columns.map { toColumnDefinition(it) },
    )
  }

  private fun toColumnDefinition(it: Column) =
    ColumnDefinition(it.name.removePrefix("\$ref:"), it.display)

  private fun toLabelDefinition(label: Label) =
    LabelDefinition(label.name.removePrefix("\$ref:"), label.display)

  private fun toFilterField(
    schemaField: SchemaField,
    allDatasets: List<Dataset>,
  ) = FieldDefinition(
    name = schemaField.name,
    display = schemaField.display,
    type = convertParameterTypeToFieldType(schemaField.type),
    filter = schemaField.filter?.let {
      map(
        filterDefinition = schemaField.filter,
        staticOptions = populateStaticOptions(
          filterDefinition = schemaField.filter,
          allDatasets = allDatasets,
        ),
      )
    },
  )

  private fun populateStaticOptions(
    filterDefinition: uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterDefinition,
    allDatasets: List<Dataset>,
  ): List<FilterOption>? = filterDefinition.dynamicOptions
    ?.takeIf { it.returnAsStaticOptions }
    ?.let { dynamicFilterOption ->
      dynamicFilterOption.dataset
        ?.let { dynamicFilterDatasetId ->
          populateStaticOptionsForFilterWithDataset(
            dynamicFilterOption,
            allDatasets,
            dynamicFilterDatasetId,
            dynamicFilterOption.maximumOptions,
          )
        }
    } ?: filterDefinition.staticOptions?.map(this::map)
}
