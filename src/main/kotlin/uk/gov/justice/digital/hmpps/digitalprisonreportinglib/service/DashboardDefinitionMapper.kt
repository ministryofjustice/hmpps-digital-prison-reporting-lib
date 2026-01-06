package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.AggregateTypeDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardBucketDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardOptionDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardSectionDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardVisualisationColumnDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardVisualisationColumnsDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardVisualisationDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardVisualisationTypeDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FieldDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FilterOption
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.UnitTypeDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ValueVisualisationColumnDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.IdentifiedHelper
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dashboard
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.DashboardVisualisation
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.DashboardVisualisationColumn
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Identified.Companion.REF_PREFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SchemaField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.alert.AlertCategoryCacheService
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.estcodesandwings.EstablishmentCodesToWingsCacheService

@Component
class DashboardDefinitionMapper(
  syncDataApiService: SyncDataApiService,
  identifiedHelper: IdentifiedHelper,
  establishmentCodesToWingsCacheService: EstablishmentCodesToWingsCacheService,
  alertCategoryCacheService: AlertCategoryCacheService,
) : DefinitionMapper(syncDataApiService, identifiedHelper, establishmentCodesToWingsCacheService, alertCategoryCacheService) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun toDashboardDefinition(
    dashboard: Dashboard,
    allDatasets: List<Dataset>,
    userToken: DprAuthAwareAuthenticationToken? = null,
    filters: Map<String, String>? = null,
  ): DashboardDefinition {
    val dataset = identifiedHelper.findOrFail(allDatasets, dashboard.dataset)

    return DashboardDefinition(
      id = dashboard.id,
      name = dashboard.name,
      description = dashboard.description,
      sections = dashboard.section.map { section ->
        DashboardSectionDefinition(
          id = section.id,
          display = section.display,
          description = section.description,
          visualisations = section.visualisation.map { visualisation ->
            DashboardVisualisationDefinition(
              id = visualisation.id,
              type = DashboardVisualisationTypeDefinition.valueOf(visualisation.type.toString()),
              display = visualisation.display,
              description = visualisation.description,
              columns = DashboardVisualisationColumnsDefinition(
                keys = visualisation.column.key?.let { mapToDashboardVisualisationColumnDefinitions(visualisation.column.key) },
                measures = mapToDashboardVisualisationColumnDefinitions(visualisation.column.measure),
                filters = visualisation.column.filter?.map { ValueVisualisationColumnDefinition(it.id.removePrefix(REF_PREFIX), it.equals) },
                expectNulls = visualisation.column.expectNull,
              ),
              options = visualisation.option?.let { mapToDashboardOptionDefinition(visualisation) },
            )
          },
        )
      },
      filterFields = mapAndAggregateAllFilters(dataset, allDatasets, userToken, filters),
    )
  }

  private fun mapToDashboardOptionDefinition(visualisation: DashboardVisualisation): DashboardOptionDefinition = DashboardOptionDefinition(
    useRagColour = visualisation.option?.useRagColour ?: false,
    baseColour = visualisation.option?.baseColour,
    buckets = visualisation.option?.bucket?.map { DashboardBucketDefinition(it.min, it.max, it.hexColour) },
    showLatest = visualisation.option?.showLatest ?: true,
    columnsAsList = visualisation.option?.columnsAsList ?: false,
    horizontal = visualisation.option?.horizontal,
    xStacked = visualisation.option?.xStacked,
    yStacked = visualisation.option?.yStacked,
  )

  private fun mapAndAggregateAllFilters(
    dataset: Dataset,
    allDatasets: List<Dataset>,
    userToken: DprAuthAwareAuthenticationToken?,
    filters: Map<String, String>?,
  ) = convertDatasetFilterFieldsToReportFields(dataset, allDatasets, userToken, filters) +
    maybeConvertParametersToReportFields(dataset.multiphaseQuery, dataset.parameters)

  private fun convertDatasetFilterFieldsToReportFields(
    dataset: Dataset,
    allDatasets: List<Dataset>,
    userToken: DprAuthAwareAuthenticationToken?,
    filters: Map<String, String>?,
  ) = dataset.schema.field
    .filter { it.filter != null }
    .map { toFilterField(it, allDatasets, userToken, dataset, filters) }

  private fun mapToDashboardVisualisationColumnDefinitions(dashboardVisualisationColumns: List<DashboardVisualisationColumn>) = dashboardVisualisationColumns.map {
    DashboardVisualisationColumnDefinition(
      it.id.removePrefix(REF_PREFIX),
      it.display,
      it.aggregate?.let { type -> AggregateTypeDefinition.valueOf(type.toString()) },
      it.unit?.let { type -> UnitTypeDefinition.valueOf(type.toString()) },
      it.displayValue,
      it.axis,
      it.optional,
    )
  }

  private fun toFilterField(
    schemaField: SchemaField,
    allDatasets: List<Dataset>,
    userToken: DprAuthAwareAuthenticationToken?,
    dashboardDataset: Dataset,
    filters: Map<String, String>?,
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
          userToken = userToken,
          dashboardDataset = dashboardDataset,
          filters = filters,
        ),
      )
    },
  )

  private fun populateStaticOptions(
    filterDefinition: FilterDefinition,
    allDatasets: List<Dataset>,
    userToken: DprAuthAwareAuthenticationToken?,
    dashboardDataset: Dataset,
    filters: Map<String, String>?,
  ): List<FilterOption>? {
    if (filterDefinition.type == FilterType.Caseloads) {
      return userToken?.getCaseLoads()?.map { FilterOption(it.id, it.name) }
    }
    return filterDefinition.dynamicOptions
      ?.takeIf { it.returnAsStaticOptions }
      ?.let { dynamicFilterOption ->
        dynamicFilterOption.dataset
          ?.let { dynamicFilterDatasetId ->
            populateStaticOptionsForFilterWithDataset(
              dynamicFilterOption,
              allDatasets,
              dynamicFilterDatasetId,
              dynamicFilterOption.maximumOptions,
              dashboardDataset,
              filters,
            )
          }
      } ?: filterDefinition.staticOptions?.map(this::map)
  }
}
