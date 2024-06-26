package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import jakarta.validation.ValidationException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.RANGE_FILTER_END_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.RANGE_FILTER_START_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.Count
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.AthenaAndRedshiftCommonRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.AthenaApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RedshiftDataApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ParameterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Schema
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SchemaField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementCancellationResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionStatus
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Service
class ConfiguredApiService(
  val productDefinitionRepository: ProductDefinitionRepository,
  val configuredApiRepository: ConfiguredApiRepository,
  val redshiftDataApiRepository: RedshiftDataApiRepository,
  val athenaApiRepository: AthenaApiRepository,
  @Value("\${URL_ENV_SUFFIX:#{null}}") val env: String? = null,
) {

  companion object {
    const val INVALID_REPORT_ID_MESSAGE = "Invalid report id provided:"
    const val INVALID_REPORT_VARIANT_ID_MESSAGE = "Invalid report variant id provided:"
    const val INVALID_FILTERS_MESSAGE = "Invalid filters provided."
    const val INVALID_STATIC_OPTIONS_MESSAGE = "Invalid static options provided."
    const val INVALID_DYNAMIC_OPTIONS_MESSAGE = "Invalid dynamic options length provided."
    const val INVALID_DYNAMIC_FILTER_MESSAGE = "Error. This filter is not a dynamic filter."
    const val MISSING_MANDATORY_FILTER_MESSAGE = "Mandatory filter value not provided:"
    const val FILTER_VALUE_DOES_NOT_MATCH_PATTERN_MESSAGE = "Filter value does not match pattern:"
    const val SCHEMA_REF_PREFIX = "\$ref:"
  }

  private val datasourceNameToRepo: Map<String, AthenaAndRedshiftCommonRepository>
    get() = mapOf(
      "datamart" to redshiftDataApiRepository,
      "nomis" to athenaApiRepository,
      "bodmis" to athenaApiRepository,
    )

  fun validateAndFetchData(
    reportId: String,
    reportVariantId: String,
    filters: Map<String, String>,
    selectedPage: Long,
    pageSize: Long,
    sortColumn: String?,
    sortedAsc: Boolean,
    userToken: DprAuthAwareAuthenticationToken?,
    reportFieldId: Set<String>? = null,
    prefix: String? = null,
    dataProductDefinitionsPath: String? = null,
    datasetForFilter: Dataset? = null,
  ): List<Map<String, Any?>> {
    val productDefinition = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId, dataProductDefinitionsPath)
    val dynamicFilter = buildAndValidateDynamicFilter(reportFieldId?.first(), prefix, productDefinition)
    val policyEngine = PolicyEngine(productDefinition.policy, userToken)
    val formulaEngine = FormulaEngine(productDefinition.report.specification?.field ?: emptyList(), env)
    return configuredApiRepository
      .executeQuery(
        query = datasetForFilter?.query ?: productDefinition.reportDataset.query,
        filters = validateAndMapFilters(productDefinition, filters) + dynamicFilter,
        selectedPage = selectedPage,
        pageSize = pageSize,
        sortColumn = datasetForFilter?.let { findSortColumn(sortColumn, it) } ?: sortColumnFromQueryOrGetDefault(productDefinition, sortColumn),
        sortedAsc = sortedAsc,
        reportId = reportId,
        policyEngineResult = datasetForFilter?.let { Policy.PolicyResult.POLICY_PERMIT } ?: policyEngine.execute(),
        dynamicFilterFieldId = reportFieldId,
        dataSourceName = productDefinition.datasource.name,
      )
      .let { records ->
        applyFormulasSelectivelyAndFormatColumns(
          records,
          productDefinition,
          formulaEngine,
          datasetForFilter,
        )
      }
  }

  private fun applyFormulasSelectivelyAndFormatColumns(
    records: List<Map<String, Any?>>,
    productDefinition: SingleReportProductDefinition,
    formulaEngine: FormulaEngine,
    datasetForFilter: Dataset?,
  ) = datasetForFilter?.let {
    records.map { row ->
      formatColumnNamesToSchemaFieldNamesCasing(row, datasetForFilter.schema.field)
    }
  } ?: formatColumnsAndApplyFormulas(records, productDefinition.reportDataset.schema.field, formulaEngine)

  fun validateAndExecuteStatementAsync(
    reportId: String,
    reportVariantId: String,
    filters: Map<String, String>,
    sortColumn: String?,
    sortedAsc: Boolean,
    userToken: DprAuthAwareAuthenticationToken?,
    reportFieldId: Set<String>? = null,
    prefix: String? = null,
    dataProductDefinitionsPath: String? = null,
  ): StatementExecutionResponse {
    val productDefinition = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId, dataProductDefinitionsPath)
    val dynamicFilter = buildAndValidateDynamicFilter(reportFieldId?.first(), prefix, productDefinition)
    val policyEngine = PolicyEngine(productDefinition.policy, userToken)
    return datasourceNameToRepo.getOrDefault(productDefinition.datasource.name.lowercase(), redshiftDataApiRepository)
      .executeQueryAsync(
        query = productDefinition.reportDataset.query,
        filters = validateAndMapFilters(productDefinition, filters) + dynamicFilter,
        sortColumn = sortColumnFromQueryOrGetDefault(productDefinition, sortColumn),
        sortedAsc = sortedAsc,
        policyEngineResult = policyEngine.execute(),
        dynamicFilterFieldId = reportFieldId,
        database = productDefinition.datasource.database,
        catalog = productDefinition.datasource.catalog,
      )
  }

  fun getStatementStatus(statementId: String, reportId: String, reportVariantId: String, dataProductDefinitionsPath: String? = null): StatementExecutionStatus {
    val productDefinition = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId, dataProductDefinitionsPath)
    return datasourceNameToRepo.getOrDefault(productDefinition.datasource.name.lowercase(), redshiftDataApiRepository).getStatementStatus(statementId)
  }

  fun getStatementResult(
    tableId: String,
    reportId: String,
    reportVariantId: String,
    dataProductDefinitionsPath: String? = null,
    selectedPage: Long,
    pageSize: Long,
  ): List<Map<String, Any?>> {
    val productDefinition = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId, dataProductDefinitionsPath)
    val formulaEngine = FormulaEngine(productDefinition.report.specification?.field ?: emptyList(), env)
    return formatColumnsAndApplyFormulas(
      redshiftDataApiRepository.getPaginatedExternalTableResult(tableId, selectedPage, pageSize),
      productDefinition.reportDataset.schema.field,
      formulaEngine,
    )
  }

  fun cancelStatementExecution(statementId: String, reportId: String, reportVariantId: String, dataProductDefinitionsPath: String? = null): StatementCancellationResponse {
    val productDefinition = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId, dataProductDefinitionsPath)
    return datasourceNameToRepo.getOrDefault(productDefinition.datasource.name.lowercase(), redshiftDataApiRepository).cancelStatementExecution(statementId)
  }

  private fun formatColumnsAndApplyFormulas(
    records: List<Map<String, Any?>>,
    schemaFields: List<SchemaField>,
    formulaEngine: FormulaEngine,
  ) = records
    .map { row -> formatColumnNamesToSchemaFieldNamesCasing(row, schemaFields) }
    .map(formulaEngine::applyFormulas)

  private fun formatColumnNamesToSchemaFieldNamesCasing(
    row: Map<String, Any?>,
    schemaFields: List<SchemaField>,
  ) = row.entries.associate { e -> transformKey(e.key, schemaFields) to e.value }

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
        query = productDefinition.reportDataset.query,
        reportId = reportId,
        policyEngineResult = policyEngine.execute(),
        dataSourceName = productDefinition.datasource.name,
      ),
    )
  }

  fun count(tableId: String): Count {
    return Count(redshiftDataApiRepository.count(tableId))
  }

  private fun calculateDefaultSortColumn(definition: SingleReportProductDefinition): String? {
    return definition.report.specification
      ?.field
      ?.firstOrNull { it.defaultSort }
      ?.name
      ?.removePrefix(SCHEMA_REF_PREFIX)
  }

  private fun sortColumnFromQueryOrGetDefault(productDefinition: SingleReportProductDefinition, sortColumn: String?): String? {
    return findSortColumn(sortColumn, productDefinition.reportDataset) ?: calculateDefaultSortColumn(productDefinition)
  }

  private fun findSortColumn(
    sortColumn: String?,
    dataset: Dataset,
  ): String? {
    return sortColumn?.let {
      dataset.schema.field.filter { schemaField -> schemaField.name == sortColumn }
        .ifEmpty { throw ValidationException("Invalid sortColumn provided: $sortColumn") }
        .first().name
    }
  }

  private fun checkMandatoryFiltersAreProvided(definition: SingleReportProductDefinition, filters: Map<String, String>) {
    definition.report.specification!!.field
      .filter { it.filter?.mandatory == true }
      .firstOrNull { !filters.keys.map(::truncateBasedOnSuffix).contains(it.name.removePrefix(SCHEMA_REF_PREFIX)) }
      ?.let { throw ValidationException("$MISSING_MANDATORY_FILTER_MESSAGE ${it.display}") }
  }

  private fun validateAndMapFilters(definition: SingleReportProductDefinition, filters: Map<String, String>): List<ConfiguredApiRepository.Filter> {
    checkMandatoryFiltersAreProvided(definition, filters)

    filters.ifEmpty { return emptyList() }

    return filters.map {
      val truncatedKey = truncateBasedOnSuffix(it.key)

      val filterDefinition = findFilterDefinition(definition, truncatedKey)

      validateValue(definition.reportDataset, filterDefinition, truncatedKey, it.value)

      ConfiguredApiRepository.Filter(
        field = truncatedKey,
        value = it.value,
        type = mapFilterType(filterDefinition, it.key, truncatedKey, definition.reportDataset.schema),
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
      type = RepositoryHelper.FilterType.DYNAMIC,
    )
  }

  private fun mapFilterType(filterDefinition: FilterDefinition, key: String, truncatedKey: String, schema: Schema): RepositoryHelper.FilterType {
    if (filterDefinition.type == FilterType.DateRange) {
      if (key.endsWith(RANGE_FILTER_START_SUFFIX)) {
        return RepositoryHelper.FilterType.DATE_RANGE_START
      } else if (key.endsWith(RANGE_FILTER_END_SUFFIX)) {
        return RepositoryHelper.FilterType.DATE_RANGE_END
      }
    }

    if (key.endsWith(RANGE_FILTER_START_SUFFIX)) {
      return RepositoryHelper.FilterType.RANGE_START
    } else if (key.endsWith(RANGE_FILTER_END_SUFFIX)) {
      return RepositoryHelper.FilterType.RANGE_END
    }
    val schemaField = schema.field.first { it.name == truncatedKey }
    if (schemaField.type == ParameterType.Boolean) {
      return RepositoryHelper.FilterType.BOOLEAN
    }

    return RepositoryHelper.FilterType.STANDARD
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
    if (filterDefinition.pattern != null && !Regex("^${filterDefinition.pattern}\$").matches(filterValue)) {
      throw ValidationException("$FILTER_VALUE_DOES_NOT_MATCH_PATTERN_MESSAGE $filterValue ${filterDefinition.pattern}")
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
