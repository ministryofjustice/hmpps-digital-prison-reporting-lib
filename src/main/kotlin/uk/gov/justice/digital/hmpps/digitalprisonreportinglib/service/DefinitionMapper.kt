package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.ExecutionContext
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DynamicFilterOption
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FieldDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FieldSource
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FieldType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FilterDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FilterOption
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FilterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.GranularityDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.QuickFilterDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.IdentifiedHelper
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.alert.AlertCategory
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.establishmentsAndWings.EstablishmentToWing
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterType.Caseloads
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.MultiphaseQuery
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Parameter
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ParameterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReferenceType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.StaticFilterOption
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.alert.AlertCategoryCacheService
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.estcodesandwings.EstablishmentCodesToWingsCacheService
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Prompt
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.temporal.ChronoUnit

abstract class DefinitionMapper(
  private val syncDataApiService: SyncDataApiService,
  identifiedHelper: IdentifiedHelper,
  val establishmentCodesToWingsCacheService: EstablishmentCodesToWingsCacheService,
  val alertCategoryCacheService: AlertCategoryCacheService,
  productDefinitionRepository: ProductDefinitionRepository,
  productDefinitionTokenPolicyChecker: ProductDefinitionTokenPolicyChecker,
) : CommonDataApiService(
  identifiedHelper = identifiedHelper,
  productDefinitionRepository = productDefinitionRepository,
  productDefinitionTokenPolicyChecker = productDefinitionTokenPolicyChecker,
) {

  companion object {
    const val DEFAULT_MAX_STATIC_OPTIONS: Long = 30
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  private val todayRegex: Regex = Regex("today\\(\\)")
  private val dateRegex: Regex = Regex("today\\((-?\\d+), ?([a-z]+)\\)", RegexOption.IGNORE_CASE)

  protected fun convertParameterTypeToFieldType(parameterType: ParameterType): FieldType = when (parameterType) {
    ParameterType.Boolean -> FieldType.Boolean
    ParameterType.Date -> FieldType.Date
    ParameterType.DateTime -> FieldType.Date
    ParameterType.Timestamp -> FieldType.Date
    ParameterType.Time -> FieldType.Time
    ParameterType.Double -> FieldType.Double
    ParameterType.Float -> FieldType.Double
    ParameterType.Integer -> FieldType.Long
    ParameterType.Long -> FieldType.Long
    ParameterType.String -> FieldType.String
  }

  protected fun map(
    filterDefinition: uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterDefinition,
    staticOptions: List<FilterOption>?,
    executionContext: ExecutionContext,
  ): FilterDefinition = FilterDefinition(
    type = populateFilterType(filterDefinition),
    staticOptions = staticOptions,
    dynamicOptions = filterDefinition.dynamicOptions?.let {
      DynamicFilterOption(
        minimumLength = filterDefinition.dynamicOptions.minimumLength,
      )
    },
    defaultValue = populateDefaultValue(filterDefinition, executionContext),
    min = replaceTokens(filterDefinition.min),
    max = replaceTokens(filterDefinition.max),
    mandatory = filterDefinition.mandatory,
    pattern = filterDefinition.pattern,
    interactive = filterDefinition.interactive ?: false,
    defaultGranularity = filterDefinition.defaultGranularity?.let { GranularityDefinition.valueOf(it.toString()) },
    defaultQuickFilterValue = filterDefinition.defaultQuickFilterValue?.let { QuickFilterDefinition.valueOf(it.toString()) },
    index = filterDefinition.index,
  )

  protected fun populateStaticOptions(
    filterDefinition: uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterDefinition,
    productDefinitionId: String,
    reportVariantId: String,
    schemaFieldName: String,
    maxStaticOptions: Long?,
    executionContext: ExecutionContext,
    dataProductDefinitionsPath: String?,
    allDatasets: List<Dataset>,
    reportDataset: Dataset,
    filters: Map<String, String>?,
  ): List<FilterOption>? {
    if (filterDefinition.type == Caseloads) {
      return executionContext.prisonCaseloadData.caseloads.map { FilterOption(it.id, it.name) }
    }
    return filterDefinition.dynamicOptions?.takeIf { it.returnAsStaticOptions }?.let { dynamicFilterOption ->
      dynamicFilterOption.dataset?.let { dynamicFilterDatasetId ->
        return populateStaticOptionsForFilterWithDataset(
          dynamicFilterOption = dynamicFilterOption,
          allDatasets = allDatasets,
          dynamicFilterDatasetId = dynamicFilterDatasetId,
          maxStaticOptions = maxStaticOptions,
          reportDataset = reportDataset,
          filters = filters,
        )
      }
        ?: populateStandardStaticOptionsForReportDefinition(
          productDefinitionId,
          reportVariantId,
          maxStaticOptions,
          schemaFieldName,
          executionContext,
          dataProductDefinitionsPath,
        )
    } ?: filterDefinition.staticOptions?.map(this::map)
  }

  protected fun maybeConvertParametersToReportFields(multiphaseQueries: List<MultiphaseQuery>, parameters: List<Parameter>?) = maybeConvertMultiphaseQueryParameters(multiphaseQueries)
    ?: maybeConvertToReportFields(parameters)

  private fun maybeConvertToReportFields(parameters: List<Parameter>?) = parameters?.map { mapParameterToField(it) } ?: emptyList()

  private fun maybeConvertMultiphaseQueryParameters(multiphaseQuery: List<MultiphaseQuery>) = multiphaseQuery.takeIf { it.size > 1 }?.let { collectAllParametersAndMapToDistinctReportFields(it) }

  private fun collectAllParametersAndMapToDistinctReportFields(queries: List<MultiphaseQuery>): List<FieldDefinition> {
    val distinctParameters =
      queries.map { maybeConvertToReportFields(it.parameters) }.filterNot { it.isEmpty() }.flatten().distinct()
    log.debug("Distinct multiphase converted fields from parameters: {}", distinctParameters)
    return distinctParameters
  }

  protected fun populateStaticOptionsForFilterWithDataset(
    dynamicFilterOption: uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.DynamicFilterOption,
    allDatasets: List<Dataset>,
    dynamicFilterDatasetId: String,
    maxStaticOptions: Long?,
    reportDataset: Dataset,
    filters: Map<String, String>?,
  ): List<FilterOption> {
    val matchingFilterDataset = identifiedHelper.findOrFail(allDatasets, dynamicFilterDatasetId)
    val matchingSchemaFieldsForFilterDataset = matchingFilterDataset.schema.field
    val nameSchemaField = identifiedHelper.findOrFail(matchingSchemaFieldsForFilterDataset, dynamicFilterOption.name)
    val displaySchemaField = identifiedHelper.findOrFail(matchingSchemaFieldsForFilterDataset, dynamicFilterOption.display)
    val prompts: List<Prompt>? = filters?.takeIf { it.isNotEmpty() }?.let {
      buildPrompts(
        query = reportDataset.query,
        promptsMap = extractPromptOnly(it, reportDataset),
        datasetParameters = reportDataset.parameters,
      )
    }
    return syncDataApiService.validateAndFetchDataForFilterWithDataset(
      pageSize = maxStaticOptions ?: DEFAULT_MAX_STATIC_OPTIONS,
      sortColumn = nameSchemaField.name,
      dataset = matchingFilterDataset,
      prompts = prompts,
    )
      .map { FilterOption(it[nameSchemaField.name].toString(), it[displaySchemaField.name].toString()) }
  }

  protected fun map(definition: StaticFilterOption): FilterOption = FilterOption(
    name = definition.name,
    display = definition.display,
  )

  private fun extractPromptOnly(
    map: Map<String, String>,
    reportDataset: Dataset,
  ): List<Map.Entry<String, String>> = partitionToPromptsAndFilters(
    map,
    extractParameters(reportDataset),
  ).first

  private fun mapParameterToField(parameter: Parameter): FieldDefinition = FieldDefinition(
    name = parameter.name,
    display = parameter.display,
    sortable = false,
    defaultsort = false,
    type = convertParameterTypeToFieldType(parameter.reportFieldType),
    mandatory = false,
    visible = false,
    filter = FilterDefinition(
      type = FilterType.valueOf(parameter.filterType.toString()),
      mandatory = parameter.mandatory,
      interactive = false,
      staticOptions = populateStaticOptionsForParameter(parameter),
      index = parameter.index,
    ),
    fieldSource = FieldSource.ParamField,
  )

  private fun populateStaticOptionsForParameter(parameter: Parameter): List<FilterOption>? = parameter.referenceType
    ?.let {
      when (it) {
        ReferenceType.ESTABLISHMENT -> mapEstablishmentsToFilterOptions()
        ReferenceType.WING -> mapWingsToFilterOptions()
        ReferenceType.ALERT -> mapAlertsToFilterOptions()
      }
    }?.takeIf { it.isNotEmpty() }

  private fun mapEstablishmentsToFilterOptions(): List<FilterOption> = establishmentCodesToWingsCacheService
    .getEstablishmentsAndPopulateCacheIfNeeded()
    .map { FilterOption(it.key, it.value.first().description) }

  private fun mapWingsToFilterOptions(): List<FilterOption> {
    val wingsFlattened = establishmentCodesToWingsCacheService
      .getEstablishmentsAndPopulateCacheIfNeeded()
      .takeIf { it.isNotEmpty() }
      ?.flatMap { it.value }
    log.debug("All wings count: ${wingsFlattened?.count() ?: 0}")
    return wingsFlattened
      ?.map { FilterOption(it.wing, it.wing) }
      ?.plus(FilterOption(EstablishmentToWing.ALL_WINGS, EstablishmentToWing.ALL_WINGS))
      ?: emptyList()
  }

  private fun mapAlertsToFilterOptions(): List<FilterOption> = alertCategoryCacheService.getAlertCodesCacheIfNeeded()
    .flatMap { alerts -> alerts.value.map { FilterOption(it.code, it.description) } }.toList()
    .plus(FilterOption(AlertCategory.ALL_ALERT, AlertCategory.ALL_ALERT))

  private fun populateStandardStaticOptionsForReportDefinition(
    productDefinitionId: String,
    reportVariantId: String,
    maxStaticOptions: Long?,
    schemaFieldName: String,
    executionContext: ExecutionContext,
    dataProductDefinitionsPath: String?,
  ) = syncDataApiService.validateAndFetchData(
    reportId = productDefinitionId,
    reportVariantId = reportVariantId,
    filters = emptyMap(),
    selectedPage = 1,
    pageSize = maxStaticOptions ?: DEFAULT_MAX_STATIC_OPTIONS,
    sortColumn = schemaFieldName,
    sortedAsc = true,
    executionContext = executionContext,
    reportFieldId = setOf(schemaFieldName),
    dataProductDefinitionsPath = dataProductDefinitionsPath,
  )
    .flatMap { it.entries }
    .map { FilterOption(it.value.toString(), it.value.toString()) }

  private fun populateFilterType(filterDefinition: uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterDefinition) = if (filterDefinition.type == Caseloads) FilterType.Multiselect else FilterType.valueOf(filterDefinition.type.toString())

  private fun populateDefaultValue(
    filterDefinition: uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterDefinition,
    executionContext: ExecutionContext,
  ) = if (filterDefinition.type == Caseloads) {
    executionContext.getCaseLoadIds().joinToString(",")
  } else {
    replaceTokens(filterDefinition.default)
  }

  private fun replaceTokens(defaultValue: String?): String? {
    if (defaultValue == null) {
      return null
    }

    val today = LocalDate.now().format(ISO_LOCAL_DATE)
    var result = defaultValue.replace(todayRegex, today)

    dateRegex.findAll(result)
      .forEach {
        val offset = it.groupValues[1].toLong()
        val resultDate = LocalDate.now().plus(offset, ChronoUnit.valueOf(it.groupValues[2].uppercase()))

        result = result.replace(it.value, resultDate.format(ISO_LOCAL_DATE))
      }

    return result
  }
}
