package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import jakarta.validation.ValidationException
import org.springframework.beans.factory.annotation.Value
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
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Schema
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SchemaField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Service
class ConfiguredApiService(
  val productDefinitionRepository: ProductDefinitionRepository,
  val configuredApiRepository: ConfiguredApiRepository,
  @Value("\${URL_ENV_SUFFIX:#{null}}") val env: String? = null,
) {

  companion object {
    const val INVALID_REPORT_ID_MESSAGE = "Invalid report id provided:"
    const val INVALID_REPORT_VARIANT_ID_MESSAGE = "Invalid report variant id provided:"
    const val INVALID_FILTERS_MESSAGE = "Invalid filters provided."
    const val INVALID_STATIC_OPTIONS_MESSAGE = "Invalid static options provided."
    const val INVALID_DYNAMIC_OPTIONS_MESSAGE = "Invalid dynamic options length provided."
    const val INVALID_DYNAMIC_FILTER_MESSAGE = "Error. This filter is not a dynamic filter."
    private const val SCHEMA_REF_PREFIX = "\$ref:"
  }

  fun validateAndFetchData(
    reportId: String,
    reportVariantId: String,
    filters: Map<String, String>,
    selectedPage: Long,
    pageSize: Long,
    sortColumn: String?,
    sortedAsc: Boolean,
    userToken: DprAuthAwareAuthenticationToken?,
    reportFieldId: String? = null,
    prefix: String? = null,
    dataProductDefinitionsPath: String? = null,
  ): List<Map<String, Any?>> {
    val productDefinition = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId, dataProductDefinitionsPath)
    val dynamicFilter = buildAndValidateDynamicFilter(reportFieldId, prefix, productDefinition)
    val policyEngine = PolicyEngine(productDefinition.policy, userToken)
    val formulaEngine = FormulaEngine(productDefinition.report.specification?.field ?: emptyList(), env)
    return configuredApiRepository
      .executeQuery(
        query = productDefinition.dataset.query,
        filters = validateAndMapFilters(productDefinition, filters) + dynamicFilter,
        selectedPage = selectedPage,
        pageSize = pageSize,
        sortColumn = sortColumnFromQueryOrGetDefault(productDefinition, sortColumn),
        sortedAsc = sortedAsc,
        reportId = reportId,
        policyEngineResult = policyEngine.execute(),
        dynamicFilterFieldId = reportFieldId,
        dataSourceName = productDefinition.datasource.name,
      )
      .map { row -> formatColumnNamesToSchemaFieldNamesCasing(row, productDefinition) }
      .map(formulaEngine::applyFormulas)
  }

  fun validateAndExecuteStatementAsync(
    reportId: String,
    reportVariantId: String,
    filters: Map<String, String>,
    sortColumn: String?,
    sortedAsc: Boolean,
    userToken: DprAuthAwareAuthenticationToken?,
    reportFieldId: String? = null,
    prefix: String? = null,
    dataProductDefinitionsPath: String? = null,
  ): String {
    val productDefinition = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId, dataProductDefinitionsPath)
    val dynamicFilter = buildAndValidateDynamicFilter(reportFieldId, prefix, productDefinition)
    val policyEngine = PolicyEngine(productDefinition.policy, userToken)
    return configuredApiRepository
      .executeQueryAsync(
        query = productDefinition.dataset.query,
        filters = validateAndMapFilters(productDefinition, filters) + dynamicFilter,
        sortColumn = sortColumnFromQueryOrGetDefault(productDefinition, sortColumn),
        sortedAsc = sortedAsc,
        reportId = reportId,
        policyEngineResult = policyEngine.execute(),
        dynamicFilterFieldId = reportFieldId,
        dataSourceName = productDefinition.datasource.name,
      )
  }

  private fun formatColumnNamesToSchemaFieldNamesCasing(
    row: Map<String, Any?>,
    productDefinition: SingleReportProductDefinition,
  ) = row.entries.associate { e -> transformKey(e.key, productDefinition.dataset.schema.field) to e.value }

  private fun buildAndValidateDynamicFilter(
    reportFieldId: String?,
    prefix: String?,
    productDefinition: SingleReportProductDefinition,
  ) = reportFieldId
    ?.let {
      prefix
        ?.let { listOf(validateAndMapFieldIdDynamicFilter(productDefinition, reportFieldId, prefix)) }
    } ?: emptyList()

  fun validateAndCount(
    reportId: String,
    reportVariantId: String,
    filters: Map<String, String>,
    userToken: DprAuthAwareAuthenticationToken?,
    dataProductDefinitionsPath: String? = null,
  ): Count {
    val productDefinition = productDefinitionRepository.getSingleReportProductDefinition(
      reportId,
      reportVariantId,
      dataProductDefinitionsPath,
    )
    val policyEngine = PolicyEngine(productDefinition.policy, userToken)
    return Count(
      configuredApiRepository.count(
        filters = validateAndMapFilters(productDefinition, filters),
        query = productDefinition.dataset.query,
        reportId = reportId,
        policyEngineResult = policyEngine.execute(),
        dataSourceName = productDefinition.datasource.name,
      ),
    )
  }

  fun calculateDefaultSortColumn(definition: SingleReportProductDefinition): String? {
    return definition.report.specification
      ?.field
      ?.firstOrNull { it.defaultSort }
      ?.name
      ?.removePrefix(SCHEMA_REF_PREFIX)
  }

  private fun sortColumnFromQueryOrGetDefault(productDefinition: SingleReportProductDefinition, sortColumn: String?): String? {
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
        type = mapFilterType(filterDefinition, it.key, truncatedKey, definition.dataset.schema),
      )
    }
  }

  private fun validateAndMapFieldIdDynamicFilter(definition: SingleReportProductDefinition, fieldId: String, prefix: String): ConfiguredApiRepository.Filter {
    val filterDefinition = findFilterDefinition(definition, fieldId)
    if (filterDefinition.dynamicOptions == null) {
      throw ValidationException(INVALID_DYNAMIC_FILTER_MESSAGE)
    }
    if (filterDefinition.dynamicOptions.minimumLength != null && prefix.length < filterDefinition.dynamicOptions.minimumLength) {
      throw ValidationException(INVALID_DYNAMIC_OPTIONS_MESSAGE)
    }
    return ConfiguredApiRepository.Filter(
      field = fieldId,
      value = prefix,
      type = ConfiguredApiRepository.FilterType.DYNAMIC,
    )
  }

  private fun mapFilterType(filterDefinition: FilterDefinition, key: String, truncatedKey: String, schema: Schema): ConfiguredApiRepository.FilterType {
    if (filterDefinition.type == FilterType.DateRange) {
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
    val schemaField = schema.field.first { it.name == truncatedKey }
    if (schemaField.type == ParameterType.Boolean) {
      return ConfiguredApiRepository.FilterType.BOOLEAN
    }

    return ConfiguredApiRepository.FilterType.STANDARD
  }

  fun findFilterDefinition(definition: SingleReportProductDefinition, filterName: String): FilterDefinition {
    val field =
      definition.report.specification?.field
        ?.firstOrNull { it.filter != null && filterName == it.name.removePrefix(SCHEMA_REF_PREFIX) }

    return field?.filter ?: throw ValidationException(INVALID_FILTERS_MESSAGE)
  }

  private fun validateValue(dataSet: Dataset, filterDefinition: FilterDefinition, filterName: String, filterValue: String) {
    validateFilterSchemaFieldType(dataSet, filterName, filterValue)
    if (filterDefinition.staticOptions != null && filterDefinition.staticOptions.none { it.name.lowercase() == filterValue.lowercase() }) {
      throw ValidationException(INVALID_STATIC_OPTIONS_MESSAGE)
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

  private fun truncateBasedOnSuffix(k: String): String {
    return if (k.endsWith(RANGE_FILTER_START_SUFFIX)) {
      k.removeSuffix(RANGE_FILTER_START_SUFFIX)
    } else if (k.endsWith(RANGE_FILTER_END_SUFFIX)) {
      k.removeSuffix(RANGE_FILTER_END_SUFFIX)
    } else {
      k
    }
  }

  private fun transformKey(key: String, schemaFields: List<SchemaField>): String {
    return schemaFields.first { it.name.lowercase() == key.lowercase() }.name
  }
}
