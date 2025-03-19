package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

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
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SchemaField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.estcodesandwings.EstablishmentCodesToWingsCacheService

@Component
class DashboardDefinitionMapper(
  syncDataApiService: SyncDataApiService,
  identifiedHelper: IdentifiedHelper,
  establishmentCodesToWingsCacheService: EstablishmentCodesToWingsCacheService,
) : DefinitionMapper(syncDataApiService, identifiedHelper, establishmentCodesToWingsCacheService) {
  fun toDashboardDefinition(dashboard: Dashboard, allDatasets: List<Dataset>, userToken: DprAuthAwareAuthenticationToken? = null): DashboardDefinition {
    val dataset = identifiedHelper.findOrFail(allDatasets, dashboard.dataset)

    return DashboardDefinition(
      id = dashboard.id,
      name = dashboard.name,
      description = dashboard.description,
      section = dashboard.section.map { section ->
        DashboardSectionDefinition(
          id = section.id,
          display = section.display,
          description = section.description,
          visualisation = section.visualisation.map { visualisation ->
            DashboardVisualisationDefinition(
              id = visualisation.id,
              type = DashboardVisualisationTypeDefinition.valueOf(visualisation.type.toString()),
              display = visualisation.display,
              description = visualisation.description,
              column = DashboardVisualisationColumnsDefinition(
                key = visualisation.column.key?.let { mapToDashboardVisualisationColumnDefinitions(visualisation.column.key) },
                measure = mapToDashboardVisualisationColumnDefinitions(visualisation.column.measure),
                filters = visualisation.column.filters?.map { ValueVisualisationColumnDefinition(it.id.removePrefix(REF_PREFIX), it.equals) },
                expectNull = visualisation.column.expectNull,
              ),
            )
          },
        )
      },
      filterFields = dataset.schema.field
        .filter { it.filter != null }
        .map { toFilterField(it, allDatasets, userToken) } + maybeConvertToReportFields(dataset.parameters),
    )
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
