package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.DataApiSyncController
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.SortDirection
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.IdentifiedHelper
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Identified.Companion.REF_PREFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ParameterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Schema
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SchemaField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleDashboardProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import java.time.LocalDate
import java.time.format.DateTimeParseException

abstract class CommonDataApiService(val identifiedHelper: IdentifiedHelper) {
  protected fun formatColumnNamesToSourceFieldNamesCasing(
    row: Map<String, Any?>,
    fieldNames: List<String>,
  ) = row.entries.associate { e -> transformKey(e.key, fieldNames) to e.value }

  protected fun buildAndValidateDynamicFilter(
    reportFieldId: String?,
    prefix: String?,
    productDefinition: SingleReportProductDefinition,
  ) = reportFieldId
    ?.let {
      prefix
        ?.let { listOf(validateAndMapFieldIdDynamicFilter(findFilterDefinition(productDefinition, reportFieldId), reportFieldId, prefix)) }
    } ?: emptyList()

  protected fun calculateDefaultSortColumn(definition: SingleReportProductDefinition): Pair<String?, Boolean> {
    val defaultSortField = definition.report.specification
      ?.field
      ?.firstOrNull { it.defaultSort }

    if (defaultSortField == null) {
      return Pair(null, false)
    }

    return Pair(defaultSortField.name.removePrefix(REF_PREFIX), defaultSortField.sortDirection == SortDirection.ASC)
  }

  protected fun sortColumnFromQueryOrGetDefault(productDefinition: SingleReportProductDefinition, sortColumn: String?, sortedAsc: Boolean?): Pair<String?, Boolean> {
    val sortColumn = findSortColumn(sortColumn, productDefinition.reportDataset)
    if (sortColumn == null) {
      val (sortColumn, sortDirection) = calculateDefaultSortColumn(productDefinition)
      val computedSortedAsc = if (sortedAsc != null && sortColumn != null) sortedAsc else sortDirection
      return Pair(sortColumn, computedSortedAsc)
    }
    return Pair(
      sortColumn,
      sortedAsc
        ?: (productDefinition.report.specification?.field?.firstOrNull { it.name.removePrefix(REF_PREFIX) == sortColumn }?.sortDirection == SortDirection.ASC),
    )
  }

  protected fun findSortColumn(
    sortColumn: String?,
    dataset: Dataset,
  ): String? = sortColumn?.let {
    dataset.schema.field.filter { schemaField -> schemaField.name == sortColumn }
      .ifEmpty { throw ValidationException("Invalid sortColumn provided: $sortColumn") }
      .first().name
  }

  private fun checkCorrectFiltersAreProvided(definition: SingleReportProductDefinition, filters: Map<String, String>, interactive: Boolean?) {
    val fieldFilters = definition.report.specification!!.field
      .map {
        val name = it.name.removePrefix(REF_PREFIX)
        name to findFilterDefinitionFromFieldsAndDataset(definition.report.specification.field, definition.reportDataset, name)
      }
      .toMap()

    checkMandatoryFiltersAreProvided(fieldFilters, interactive, filters)
    checkCorrectFiltersAreProvidedForStage(fieldFilters, interactive, filters)
  }

  private fun checkCorrectFiltersAreProvided(definition: SingleDashboardProductDefinition, filters: Map<String, String>, interactive: Boolean) {
    val fieldFilters = definition.dashboardDataset.schema.field
      .map { it.name.removePrefix(REF_PREFIX) to findFilterDefinitionFromFieldsAndDataset(null, definition.dashboardDataset, it.name) }
      .toMap()

    checkMandatoryFiltersAreProvided(fieldFilters, interactive, filters)
    checkCorrectFiltersAreProvidedForStage(fieldFilters, interactive, filters)
  }

  private fun checkMandatoryFiltersAreProvided(
    fieldFilters: Map<String, FilterDefinition?>,
    interactive: Boolean?,
    filters: Map<String, String>,
  ) {
    fieldFilters.keys
      .firstOrNull {
        fieldFilters[it] != null &&
          fieldFilters[it]?.mandatory == true &&
          isAtTheSameInteractiveStage(interactive, fieldFilters[it]) &&
          isNotInTheProvidedFilters(filters, it)
      }
      ?.let { throw ValidationException("${AsyncDataApiService.MISSING_MANDATORY_FILTER_MESSAGE} $it") }
  }

  private fun isNotInTheProvidedFilters(filters: Map<String, String>, it: String) = !filters.keys.map(::truncateBasedOnSuffix).contains(it)

  private fun isAtTheSameInteractiveStage(
    interactive: Boolean?,
    fieldFilterDefinition: FilterDefinition?,
  ) = (interactive == null || (fieldFilterDefinition?.interactive ?: false) == interactive)

  private fun checkCorrectFiltersAreProvidedForStage(
    fieldFilters: Map<String, FilterDefinition?>,
    interactive: Boolean?,
    filters: Map<String, String>,
  ) {
    if (interactive != null) {
      fieldFilters.keys
        .firstOrNull {
          fieldFilters[it] != null &&
            filters.keys.map(::truncateBasedOnSuffix).contains(it) &&
            (fieldFilters[it]!!.interactive ?: false) != interactive
        }
        ?.let { throw ValidationException("Filter provided for wrong stage. Expected stage: interactive=$interactive, filter stage: interactive=${fieldFilters[it]!!.interactive ?: false}, field name: $it") }
    }
  }

  protected fun validateAndMapFilters(definition: SingleReportProductDefinition, filters: Map<String, String>, interactive: Boolean?, reportFieldId: Set<String>? = null): List<ConfiguredApiRepository.Filter> {
    if (reportFieldId == null) {
      checkCorrectFiltersAreProvided(definition, filters, interactive)
    }

    filters.ifEmpty { return emptyList() }

    return filters.map {
      val truncatedKey = truncateBasedOnSuffix(it.key)

      val filterDefinition = findFilterDefinition(definition, truncatedKey)

      val filterType = mapFilterTypeAndValidateValue(
        filterDefinition = filterDefinition,
        filter = it,
        truncatedKey = truncatedKey,
        dataset = definition.reportDataset,
        reportFieldId = reportFieldId,
      )
      ConfiguredApiRepository.Filter(
        field = truncatedKey,
        value = it.value,
        type = filterType,
      )
    }
  }
  protected fun validateAndMapFilters(definition: SingleDashboardProductDefinition, filters: Map<String, String>, interactive: Boolean): List<ConfiguredApiRepository.Filter> {
    checkCorrectFiltersAreProvided(definition, filters, interactive)

    filters.ifEmpty { return emptyList() }

    return filters.map {
      val truncatedKey = truncateBasedOnSuffix(it.key)

      val filterDefinition = findFilterDefinition(definition, truncatedKey)

      val filterType = mapFilterTypeAndValidateValue(
        filterDefinition = filterDefinition,
        filter = it,
        truncatedKey = truncatedKey,
        dataset = definition.dashboardDataset,
      )

      ConfiguredApiRepository.Filter(
        field = truncatedKey,
        value = it.value,
        type = filterType,
      )
    }
  }

  private fun validateAndMapFieldIdDynamicFilter(filterDefinition: FilterDefinition, fieldId: String, prefix: String): ConfiguredApiRepository.Filter {
    if (filterDefinition.dynamicOptions == null) {
      throw ValidationException(AsyncDataApiService.INVALID_DYNAMIC_FILTER_MESSAGE)
    }
    if (filterDefinition.dynamicOptions.minimumLength != null && prefix.length < filterDefinition.dynamicOptions.minimumLength) {
      throw ValidationException(AsyncDataApiService.INVALID_DYNAMIC_OPTIONS_MESSAGE)
    }
    return ConfiguredApiRepository.Filter(
      field = fieldId,
      value = prefix,
      type = RepositoryHelper.FilterType.DYNAMIC,
    )
  }

  private fun mapFilterType(filterDefinition: FilterDefinition, key: String, truncatedKey: String, schema: Schema): RepositoryHelper.FilterType {
    if (filterDefinition.type == FilterType.Caseloads || filterDefinition.type == FilterType.Multiselect) {
      return RepositoryHelper.FilterType.MULTISELECT
    }
    if (filterDefinition.type == FilterType.DateRange) {
      if (key.endsWith(DataApiSyncController.FiltersPrefix.RANGE_FILTER_START_SUFFIX)) {
        return RepositoryHelper.FilterType.DATE_RANGE_START
      } else if (key.endsWith(DataApiSyncController.FiltersPrefix.RANGE_FILTER_END_SUFFIX)) {
        return RepositoryHelper.FilterType.DATE_RANGE_END
      }
    }

    if (key.endsWith(DataApiSyncController.FiltersPrefix.RANGE_FILTER_START_SUFFIX)) {
      return RepositoryHelper.FilterType.RANGE_START
    } else if (key.endsWith(DataApiSyncController.FiltersPrefix.RANGE_FILTER_END_SUFFIX)) {
      return RepositoryHelper.FilterType.RANGE_END
    }
    val schemaField = schema.field.first { it.name == truncatedKey }
    if (schemaField.type == ParameterType.Boolean) {
      return RepositoryHelper.FilterType.BOOLEAN
    }

    return RepositoryHelper.FilterType.STANDARD
  }

  private fun mapFilterTypeAndValidateValue(
    filterDefinition: FilterDefinition,
    filter: Map.Entry<String, String>,
    truncatedKey: String,
    dataset: Dataset,
    reportFieldId: Set<String>? = null,
  ): RepositoryHelper.FilterType {
    val filterType = mapFilterType(filterDefinition, filter.key, truncatedKey, dataset.schema)
    if (filterType == RepositoryHelper.FilterType.MULTISELECT) {
      filter.value.split(",")
        .forEach { v -> validateValue(dataset, filterDefinition, truncatedKey, v, reportFieldId) }
    } else {
      validateValue(dataset, filterDefinition, truncatedKey, filter.value, reportFieldId)
    }
    return filterType
  }

  private fun findFilterDefinition(definition: SingleReportProductDefinition, filterName: String): FilterDefinition {
    val filter = findFilterDefinitionFromFieldsAndDataset(definition.report.specification?.field, definition.reportDataset, filterName)

    return filter ?: throw ValidationException(AsyncDataApiService.INVALID_FILTERS_MESSAGE)
  }

  private fun findFilterDefinition(definition: SingleDashboardProductDefinition, filterName: String): FilterDefinition {
    val filter = findFilterDefinitionFromFieldsAndDataset(null, definition.dashboardDataset, filterName)

    return filter ?: throw ValidationException(AsyncDataApiService.INVALID_FILTERS_MESSAGE)
  }

  private fun findFilterDefinitionFromFieldsAndDataset(fields: List<ReportField>?, dataset: Dataset, filterName: String): FilterDefinition? {
    val fieldFilter = identifiedHelper.findOrNull(fields, filterName)?.filter

    val datasetFilter = findFilterDefinitionFromDataset(dataset, filterName)

    return fieldFilter ?: datasetFilter
  }

  private fun findFilterDefinitionFromDataset(dataset: Dataset, filterName: String): FilterDefinition? = identifiedHelper.findOrNull<SchemaField>(dataset.schema.field, filterName)?.filter

  private fun validateValue(dataSet: Dataset, filterDefinition: FilterDefinition, filterName: String, filterValue: String, reportFieldId: Set<String>?) {
    validateFilterSchemaFieldType(dataSet, filterName, filterValue)
    if (filterDefinition.staticOptions != null && filterDefinition.staticOptions.none { it.name.lowercase() == filterValue.lowercase() }) {
      throw ValidationException(AsyncDataApiService.INVALID_STATIC_OPTIONS_MESSAGE)
    }
    if (filterDefinition.pattern != null &&
      (reportFieldId == null || !reportFieldId.contains(filterName)) &&
      !Regex("^${filterDefinition.pattern}\$").matches(filterValue)
    ) {
      throw ValidationException("${AsyncDataApiService.FILTER_VALUE_DOES_NOT_MATCH_PATTERN_MESSAGE} $filterValue ${filterDefinition.pattern}")
    }
  }

  private fun validateFilterSchemaFieldType(dataSet: Dataset, key: String, value: String) {
    val schemaField = dataSet.schema.field.first { it.name == key }
    if (schemaField.type == ParameterType.Long) {
      try {
        value.toLong()
      } catch (e: NumberFormatException) {
        throw ValidationException("Invalid value $value for filter $key. Cannot be parsed as a number.")
      }
    } else if (schemaField.type == ParameterType.Date) {
      try {
        LocalDate.parse(value)
      } catch (e: DateTimeParseException) {
        throw ValidationException("Invalid value $value for filter $key. Cannot be parsed as a date.")
      }
    } else if (schemaField.type == ParameterType.Boolean) {
      if (isNotABoolean(value)) {
        throw ValidationException("Invalid value $value for filter $key. Cannot be parsed as a boolean.")
      }
    }
  }

  private fun isNotABoolean(value: String) = value.lowercase() != "true" && value.lowercase() != "false"

  private fun truncateBasedOnSuffix(k: String): String = if (k.endsWith(DataApiSyncController.FiltersPrefix.RANGE_FILTER_START_SUFFIX)) {
    k.removeSuffix(DataApiSyncController.FiltersPrefix.RANGE_FILTER_START_SUFFIX)
  } else if (k.endsWith(DataApiSyncController.FiltersPrefix.RANGE_FILTER_END_SUFFIX)) {
    k.removeSuffix(DataApiSyncController.FiltersPrefix.RANGE_FILTER_END_SUFFIX)
  } else {
    k
  }

  private fun transformKey(key: String, fieldNames: List<String>): String = fieldNames.firstOrNull { it.lowercase() == key.lowercase() }
    ?: throw ValidationException("The DPD is missing schema field: $key.")
}
