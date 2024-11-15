package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.DataApiSyncController
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.*
import java.time.LocalDate
import java.time.format.DateTimeParseException

abstract class CommonDataApiService {
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
        ?.let { listOf(validateAndMapFieldIdDynamicFilter(productDefinition, reportFieldId, prefix)) }
    } ?: emptyList()

  protected fun calculateDefaultSortColumn(definition: SingleReportProductDefinition): String? {
    return definition.report.specification
      ?.field
      ?.firstOrNull { it.defaultSort }
      ?.name
      ?.removePrefix(AsyncDataApiService.SCHEMA_REF_PREFIX)
  }

  protected fun sortColumnFromQueryOrGetDefault(productDefinition: SingleReportProductDefinition, sortColumn: String?): String? {
    return findSortColumn(sortColumn, productDefinition.reportDataset) ?: calculateDefaultSortColumn(productDefinition)
  }

  protected fun findSortColumn(
    sortColumn: String?,
    dataset: Dataset,
  ): String? {
    return sortColumn?.let {
      dataset.schema.field.filter { schemaField -> schemaField.name == sortColumn }
        .ifEmpty { throw ValidationException("Invalid sortColumn provided: $sortColumn") }
        .first().name
    }
  }

  protected fun checkMandatoryFiltersAreProvided(definition: SingleReportProductDefinition, filters: Map<String, String>) {
    definition.report.specification!!.field
      .filter { it.filter?.mandatory == true }
      .firstOrNull { !filters.keys.map(::truncateBasedOnSuffix).contains(it.name.removePrefix(AsyncDataApiService.SCHEMA_REF_PREFIX)) }
      ?.let { throw ValidationException("${AsyncDataApiService.MISSING_MANDATORY_FILTER_MESSAGE} ${it.display}") }
  }

  protected fun checkMandatoryFiltersAreProvided(definition: SingleDashboardProductDefinition, filters: Map<String, String>, interactive: Boolean) {
    definition.dashboardDataset.schema.field
      .filter { it.filter?.interactive == interactive && it.filter.mandatory }
      .firstOrNull { !filters.keys.map(::truncateBasedOnSuffix).contains(it.name.removePrefix(AsyncDataApiService.SCHEMA_REF_PREFIX)) }
      ?.let { throw ValidationException("${AsyncDataApiService.MISSING_MANDATORY_FILTER_MESSAGE} ${it.display}") }
  }

  protected fun validateAndMapFilters(definition: SingleReportProductDefinition, filters: Map<String, String>, reportFieldId: Set<String>? = null): List<ConfiguredApiRepository.Filter> {
    if (reportFieldId == null) {
      checkMandatoryFiltersAreProvided(definition, filters)
    }

    filters.ifEmpty { return emptyList() }

    return filters.map {
      val truncatedKey = truncateBasedOnSuffix(it.key)

      val filterDefinition = findFilterDefinition(definition, truncatedKey)

      validateValue(definition.reportDataset, filterDefinition, truncatedKey, it.value, reportFieldId)

      ConfiguredApiRepository.Filter(
        field = truncatedKey,
        value = it.value,
        type = mapFilterType(filterDefinition, it.key, truncatedKey, definition.reportDataset.schema),
      )
    }
  }

  protected fun validateAndMapFilters(definition: SingleDashboardProductDefinition, filters: Map<String, String>, interactive: Boolean): List<ConfiguredApiRepository.Filter> {
    checkMandatoryFiltersAreProvided(definition, filters, interactive)

    filters.ifEmpty { return emptyList() }

    return filters.map {
      val truncatedKey = truncateBasedOnSuffix(it.key)

      val filterDefinition = findFilterDefinition(definition, truncatedKey)

      validateValue(definition.dashboardDataset, filterDefinition, truncatedKey, it.value, null)

      ConfiguredApiRepository.Filter(
        field = truncatedKey,
        value = it.value,
        type = mapFilterType(filterDefinition, it.key, truncatedKey, definition.dashboardDataset.schema),
      )
    }
  }

  protected fun validateAndMapFieldIdDynamicFilter(definition: SingleReportProductDefinition, fieldId: String, prefix: String): ConfiguredApiRepository.Filter {
    val filterDefinition = findFilterDefinition(definition, fieldId)
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

  protected fun mapFilterType(filterDefinition: FilterDefinition, key: String, truncatedKey: String, schema: Schema): RepositoryHelper.FilterType {
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

  fun findFilterDefinition(definition: SingleReportProductDefinition, filterName: String): FilterDefinition {
    val fieldFilter =
      definition.report.specification?.field
        ?.firstOrNull { it.filter != null && filterName == it.name.removePrefix(AsyncDataApiService.SCHEMA_REF_PREFIX) }?.filter

    val datasetFilter = findFilterDefinition(definition.reportDataset, filterName)

    return fieldFilter ?: datasetFilter ?: throw ValidationException(AsyncDataApiService.INVALID_FILTERS_MESSAGE)
  }

  fun findFilterDefinition(definition: SingleDashboardProductDefinition, filterName: String): FilterDefinition {
    val datasetFilter = findFilterDefinition(definition.dashboardDataset, filterName)

    return datasetFilter ?: throw ValidationException(AsyncDataApiService.INVALID_FILTERS_MESSAGE)
  }

  fun findFilterDefinition(dataset: Dataset, filterName: String): FilterDefinition? {
    val datasetFilter = dataset.schema.field.firstOrNull { it.filter != null && filterName == it.name }?.filter

    return datasetFilter
  }

  protected fun validateValue(dataSet: Dataset, filterDefinition: FilterDefinition, filterName: String, filterValue: String, reportFieldId: Set<String>?) {
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

  protected fun validateFilterSchemaFieldType(dataSet: Dataset, key: String, value: String) {
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

  protected fun isNotABoolean(value: String) = value.lowercase() != "true" && value.lowercase() != "false"

  protected fun truncateBasedOnSuffix(k: String): String {
    return if (k.endsWith(DataApiSyncController.FiltersPrefix.RANGE_FILTER_START_SUFFIX)) {
      k.removeSuffix(DataApiSyncController.FiltersPrefix.RANGE_FILTER_START_SUFFIX)
    } else if (k.endsWith(DataApiSyncController.FiltersPrefix.RANGE_FILTER_END_SUFFIX)) {
      k.removeSuffix(DataApiSyncController.FiltersPrefix.RANGE_FILTER_END_SUFFIX)
    } else {
      k
    }
  }

  protected fun transformKey(key: String, fieldNames: List<String>): String {
    return fieldNames.firstOrNull { it.lowercase() == key.lowercase() }
      ?: throw ValidationException("The DPD is missing schema field: $key.")
  }
}
