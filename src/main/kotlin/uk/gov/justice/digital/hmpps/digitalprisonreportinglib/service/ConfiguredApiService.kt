package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.RANGE_FILTER_END_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.RANGE_FILTER_START_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.Count
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ParameterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SchemaField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Service
class ConfiguredApiService(
  val productDefinitionRepository: ProductDefinitionRepository,
  val configuredApiRepository: ConfiguredApiRepository,
) {

  companion object {
    const val INVALID_REPORT_ID_MESSAGE = "Invalid report id provided:"
    const val INVALID_REPORT_VARIANT_ID_MESSAGE = "Invalid report variant id provided:"
    const val INVALID_FILTERS_MESSAGE = "Invalid filters provided."
    const val INVALID_STATIC_OPTIONS_MESSAGE = "Invalid static options provided."
    private const val schemaRefPrefix = "\$ref:"
  }

  fun validateAndFetchData(
    reportId: String,
    reportVariantId: String,
    filters: Map<String, String>,
    selectedPage: Long,
    pageSize: Long,
    sortColumn: String?,
    sortedAsc: Boolean,
    userCaseloads: List<String>,
  ): List<Map<String, Any>> {
    val productDefinition = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId)
    val validatedSortColumn = validateSortColumnOrGetDefault(productDefinition, sortColumn)

    return formatToSchemaFieldsCasing(
      configuredApiRepository
        .executeQuery(
          productDefinition.dataset.query,
          validateAndMapFilters(productDefinition, filters),
          selectedPage,
          pageSize,
          validatedSortColumn,
          sortedAsc,
          userCaseloads,
          getCaseloadFields(productDefinition.dataset),
          reportId,
        ),
      productDefinition.dataset.schema.field,
    )
  }

  private fun getCaseloadFields(dataSet: Dataset) =
    dataSet.schema.field.filter { it.caseload }.map { it.name }

  fun validateAndCount(
    reportId: String,
    reportVariantId: String,
    filters: Map<String, String>,
    userCaseloads: List<String>,
  ): Count {
    val productDefinition = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId)

    return Count(
      configuredApiRepository.count(
        validateAndMapFilters(productDefinition, filters),
        productDefinition.dataset.query,
        userCaseloads,
        getCaseloadFields(productDefinition.dataset),
        reportId,
      ),
    )
  }

  fun calculateDefaultSortColumn(definition: SingleReportProductDefinition): String {
    return definition.report.specification
      ?.field
      ?.first { it.`default-sort` }
      ?.name
      ?.removePrefix(schemaRefPrefix)
      ?: throw ValidationException("Could not find default sort column for reportId: ${definition.id}, reportVariantId: ${definition.report.id}")
  }

  private fun validateSortColumnOrGetDefault(productDefinition: SingleReportProductDefinition, sortColumn: String?): String {
    return sortColumn?.let {
      productDefinition.dataset.schema.field.filter { schemaField -> schemaField.name == sortColumn }
        .ifEmpty { throw ValidationException("Invalid sortColumn provided: $sortColumn") }
        .first().name
    } ?: calculateDefaultSortColumn(productDefinition)
  }

  private fun validateAndMapFilters(definition: SingleReportProductDefinition, filters: Map<String, String>): List<ConfiguredApiRepository.Filter> {
    filters.ifEmpty { return emptyList() }

    return filters.map {
      val truncatedKey = truncateBasedOnSuffix(it.key)

      val filterDefinition = findFilterDefinition(definition, truncatedKey)

      validateValue(definition.dataset, filterDefinition, truncatedKey, it.value)

      ConfiguredApiRepository.Filter(
        field = truncatedKey,
        value = it.value,
        type = mapFilterType(filterDefinition, it.key),
      )
    }
  }

  private fun mapFilterType(filterDefinition: FilterDefinition, key: String): ConfiguredApiRepository.FilterType {
    if (filterDefinition.type == FilterType.DATE_RANGE) {
      if (key.endsWith(RANGE_FILTER_START_SUFFIX)) {
        return ConfiguredApiRepository.FilterType.DATE_RANGE_START
      } else if (key.endsWith(RANGE_FILTER_END_SUFFIX)) {
        return ConfiguredApiRepository.FilterType.DATE_RANGE_END
      }
    }

    if (key.endsWith(RANGE_FILTER_START_SUFFIX)) {
      return ConfiguredApiRepository.FilterType.RANGE_START
    } else if (key.endsWith(RANGE_FILTER_END_SUFFIX)) {
      return ConfiguredApiRepository.FilterType.RANGE_END
    }

    return ConfiguredApiRepository.FilterType.STANDARD
  }

  fun findFilterDefinition(definition: SingleReportProductDefinition, key: String): FilterDefinition {
    val field =
      definition.report.specification?.field
        ?.firstOrNull { it.filter != null && key == it.name.removePrefix(schemaRefPrefix) }

    return field?.filter ?: throw ValidationException(INVALID_FILTERS_MESSAGE)
  }

  private fun validateValue(dataSet: Dataset, filterDefinition: FilterDefinition, key: String, value: String) {
    validateFilterType(dataSet, key, value)
    if (filterDefinition.staticOptions != null && filterDefinition.staticOptions.none { it.name.lowercase() == value.lowercase() }) {
      throw ValidationException(INVALID_STATIC_OPTIONS_MESSAGE)
    }
  }

  private fun validateFilterType(dataSet: Dataset, key: String, value: String) {
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
    }
  }

  private fun truncateBasedOnSuffix(k: String): String {
    return if (k.endsWith(RANGE_FILTER_START_SUFFIX)) {
      k.removeSuffix(RANGE_FILTER_START_SUFFIX)
    } else if (k.endsWith(RANGE_FILTER_END_SUFFIX)) {
      k.removeSuffix(RANGE_FILTER_END_SUFFIX)
    } else {
      k
    }
  }

  private fun formatToSchemaFieldsCasing(resultRows: List<Map<String, Any>>, schemaFields: List<SchemaField>): List<Map<String, Any>> {
    return resultRows
      .map { row -> row.entries.associate { e -> transformKey(e.key, schemaFields) to e.value } }
  }

  private fun transformKey(key: String, schemaFields: List<SchemaField>): String {
    return schemaFields.first { it.name.lowercase() == key.lowercase() }.name
  }
}
