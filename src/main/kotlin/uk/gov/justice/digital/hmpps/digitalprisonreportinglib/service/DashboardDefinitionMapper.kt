package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.AggregateTypeDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ChartDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ChartTypeDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ColumnDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardSectionDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardVisualisationColumnDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardVisualisationColumnsDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardVisualisationDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardVisualisationTypeDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FieldDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FilterOption
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.LabelDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.MetricDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.UnitTypeDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ValueVisualisationColumnDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.IdentifiedHelper
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Chart
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Column
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dashboard
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.DashboardVisualisationColumn
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Label
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Metric
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SchemaField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.estcodesandwings.EstablishmentCodesToWingsCacheService

@Component
class DashboardDefinitionMapper(
  syncDataApiService: SyncDataApiService,
  identifiedHelper: IdentifiedHelper,
  establishmentCodesToWingsCacheService: EstablishmentCodesToWingsCacheService,
) : DefinitionMapper(syncDataApiService, identifiedHelper, establishmentCodesToWingsCacheService) {
  fun toDashboardDefinition(dashboard: Dashboard, allDatasets: List<Dataset>): DashboardDefinition {
    val dataset = identifiedHelper.findOrFail(allDatasets, dashboard.dataset)

    return DashboardDefinition(
      id = dashboard.id,
      name = dashboard.name,
      description = dashboard.description,
      sections = dashboard.sections.map { section ->
        DashboardSectionDefinition(
          id = section.id,
          display = section.display,
          description = section.description,
          visualisations = section.visualisations.map { visualisation ->
            DashboardVisualisationDefinition(
              id = visualisation.id,
              type = DashboardVisualisationTypeDefinition.valueOf(visualisation.type.toString()),
              display = visualisation.display,
              description = visualisation.description,
              columns = DashboardVisualisationColumnsDefinition(
                keys = visualisation.columns.keys?.let { mapToDashboardVisualisationColumnDefinitions(visualisation.columns.keys) },
                measures = mapToDashboardVisualisationColumnDefinitions(visualisation.columns.measures),
                filters = visualisation.columns.filters?.map { ValueVisualisationColumnDefinition(it.id, it.equals) },
                expectNulls = visualisation.columns.expectNulls,
              ),
            )
          },
        )
      },
      filterFields = dataset.schema.field
        .filter { it.filter != null }
        .map { toFilterField(it, allDatasets) } + maybeConvertToReportFields(dataset.parameters),
    )
  }

  private fun mapToDashboardVisualisationColumnDefinitions(dashboardVisualisationColumns: List<DashboardVisualisationColumn>) =
    dashboardVisualisationColumns.map {
      DashboardVisualisationColumnDefinition(
        it.id,
        it.display,
        it.aggregate?.let { type -> AggregateTypeDefinition.valueOf(type.toString()) },
        it.unit?.let { type -> UnitTypeDefinition.valueOf(type.toString()) },
        it.displayValue,
        it.axis,
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
