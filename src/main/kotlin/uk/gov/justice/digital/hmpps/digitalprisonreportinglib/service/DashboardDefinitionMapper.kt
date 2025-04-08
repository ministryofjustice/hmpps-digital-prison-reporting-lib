package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.AggregateTypeDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardDefinition
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
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.DashboardVisualisationColumn
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Identified.Companion.REF_PREFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.MultiphaseQuery
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SchemaField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.estcodesandwings.EstablishmentCodesToWingsCacheService

@Component
class DashboardDefinitionMapper(
  syncDataApiService: SyncDataApiService,
  identifiedHelper: IdentifiedHelper,
  establishmentCodesToWingsCacheService: EstablishmentCodesToWingsCacheService,
) : DefinitionMapper(syncDataApiService, identifiedHelper, establishmentCodesToWingsCacheService) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun toDashboardDefinition(dashboard: Dashboard, allDatasets: List<Dataset>, userToken: DprAuthAwareAuthenticationToken? = null): DashboardDefinition {
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
                filters = visualisation.column.filters?.map { ValueVisualisationColumnDefinition(it.id.removePrefix(REF_PREFIX), it.equals) },
                expectNulls = visualisation.column.expectNull,
              ),
            )
          },
        )
      },
      filterFields = dataset.schema.field
        .filter { it.filter != null }
        .map { toFilterField(it, allDatasets, userToken) } +
        (dataset.multiphaseQuery?.takeIf { it.isNotEmpty() }?.let { collectAllParametersAndMapToDistinctReportFields(it) } ?: maybeConvertToReportFields(dataset.parameters)),
    )
  }

  private fun collectAllParametersAndMapToDistinctReportFields(queries: List<MultiphaseQuery>): List<FieldDefinition> {
    val distinctParameters =
      queries.map { maybeConvertToReportFields(it.parameters) }.filterNot { it.isEmpty() }.flatten().distinct()
    log.debug("Distinct multiphase converted fields from parameters: $distinctParameters")
    return distinctParameters
  }

  private fun mapToDashboardVisualisationColumnDefinitions(dashboardVisualisationColumns: List<DashboardVisualisationColumn>) = dashboardVisualisationColumns.map {
    DashboardVisualisationColumnDefinition(
      it.id.removePrefix(REF_PREFIX),
      it.display,
      it.aggregate?.let { type -> AggregateTypeDefinition.valueOf(type.toString()) },
      it.unit?.let { type -> UnitTypeDefinition.valueOf(type.toString()) },
      it.displayValue,
      it.axis,
    )
  }

  private fun toFilterField(
    schemaField: SchemaField,
    allDatasets: List<Dataset>,
    userToken: DprAuthAwareAuthenticationToken?,
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
        ),
      )
    },
  )

  private fun populateStaticOptions(
    filterDefinition: FilterDefinition,
    allDatasets: List<Dataset>,
    userToken: DprAuthAwareAuthenticationToken?,
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
            )
          }
      } ?: filterDefinition.staticOptions?.map(this::map)
  }
}
