package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import jakarta.validation.ValidationException
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.UncategorizedSQLException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.Count
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.AthenaAndRedshiftCommonRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.AthenaApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.DatasetHelper
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RedshiftDataApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SchemaField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementCancellationResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionStatus
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Prompt

@Service
class AsyncDataApiService(
  val productDefinitionRepository: ProductDefinitionRepository,
  val configuredApiRepository: ConfiguredApiRepository,
  val redshiftDataApiRepository: RedshiftDataApiRepository,
  val athenaApiRepository: AthenaApiRepository,
  val tableIdGenerator: TableIdGenerator,
  val datasetHelper: DatasetHelper,
  @Value("\${URL_ENV_SUFFIX:#{null}}") val env: String? = null,
) : CommonDataApiService() {

  companion object {
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
    val productDefinition = productDefinitionRepository.getSingleReportProductDefinition(
      reportId,
      reportVariantId,
      dataProductDefinitionsPath,
    )
    val dynamicFilter = buildAndValidateDynamicFilter(reportFieldId?.first(), prefix, productDefinition)
    val policyEngine = PolicyEngine(productDefinition.policy, userToken)
    val (promptsMap, filtersOnly) = partitionToPromptsAndFilters(filters, productDefinition)
    return getRepo(productDefinition)
      .executeQueryAsync(
        productDefinition = productDefinition,
        filters = validateAndMapFilters(productDefinition, toMap(filtersOnly)) + dynamicFilter,
        sortColumn = sortColumnFromQueryOrGetDefault(productDefinition, sortColumn),
        sortedAsc = sortedAsc,
        policyEngineResult = policyEngine.execute(),
        dynamicFilterFieldId = reportFieldId,
        prompts = buildPrompts(promptsMap, productDefinition),
        userToken = userToken,
      )
  }

  fun validateAndExecuteStatementAsync(
    reportId: String,
    dashboardId: String,
    userToken: DprAuthAwareAuthenticationToken?,
    dataProductDefinitionsPath: String? = null,
  ): StatementExecutionResponse {
    val productDefinition = productDefinitionRepository.getSingleDashboardProductDefinition(
      definitionId = reportId,
      dashboardId = dashboardId,
      dataProductDefinitionsPath = dataProductDefinitionsPath,
    )
    val policyEngine = PolicyEngine(productDefinition.policy, userToken)
    return redshiftDataApiRepository
      .executeQueryAsync(
        productDefinition = productDefinition,
        policyEngineResult = policyEngine.execute(),
      )
  }

  fun getStatementStatus(statementId: String, reportId: String, reportVariantId: String, dataProductDefinitionsPath: String? = null): StatementExecutionStatus {
    val productDefinition = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId, dataProductDefinitionsPath)
    return getRepo(productDefinition).getStatementStatus(statementId)
  }

  fun getStatementStatus(statementId: String): StatementExecutionStatus {
    return redshiftDataApiRepository.getStatementStatus(statementId)
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

  fun getDashboardStatementResult(
    tableId: String,
    reportId: String,
    dashboardId: String,
    dataProductDefinitionsPath: String? = null,
    selectedPage: Long,
    pageSize: Long,
  ): List<Map<String, Any?>> {
    val productDefinition = productDefinitionRepository.getSingleDashboardProductDefinition(reportId, dashboardId, dataProductDefinitionsPath)
    return redshiftDataApiRepository.getPaginatedExternalTableResult(tableId, selectedPage, pageSize)
      .map { row -> formatColumnNamesToSourceFieldNamesCasing(row, productDefinition.dashboardDataset.schema.field.map(SchemaField::name)) }
  }

  fun getSummaryResult(
    tableId: String,
    summaryId: String,
    reportId: String,
    reportVariantId: String,
    dataProductDefinitionsPath: String? = null,
  ): List<Map<String, Any?>> {
    val productDefinition = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId, dataProductDefinitionsPath)

    val summary = productDefinition.report.summary?.find { it.id == summaryId }
      ?: throw ValidationException("Invalid summary ID: $summaryId")

    val dataset = datasetHelper.findDataset(productDefinition.allDatasets, summary.dataset)
    val tableSummaryId = tableIdGenerator.getTableSummaryId(tableId, summaryId)

    // Request data from the summary table.
    // If it doesn't exist, create it (waiting for creation to complete).
    val results = try {
      redshiftDataApiRepository.getFullExternalTableResult(tableSummaryId)
    } catch (e: UncategorizedSQLException) {
      if (e.message?.contains("Entity Not Found") == true) {
        configuredApiRepository.createSummaryTable(tableId, summaryId, dataset.query, productDefinition.datasource.name)
        redshiftDataApiRepository.getFullExternalTableResult(tableSummaryId)
      } else {
        throw e
      }
    }

    return results.map {
      formatColumnNamesToSourceFieldNamesCasing(it, dataset.schema.field.map(SchemaField::name))
    }
  }

  fun cancelStatementExecution(statementId: String, reportId: String, reportVariantId: String, dataProductDefinitionsPath: String? = null): StatementCancellationResponse {
    val productDefinition = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId, dataProductDefinitionsPath)
    return getRepo(productDefinition).cancelStatementExecution(statementId)
  }

  fun count(tableId: String): Count {
    return Count(redshiftDataApiRepository.count(tableId))
  }

  private fun getRepo(productDefinition: SingleReportProductDefinition): AthenaAndRedshiftCommonRepository =
    datasourceNameToRepo.getOrDefault(productDefinition.datasource.name.lowercase(), redshiftDataApiRepository)

  private fun buildPrompts(
    prompts: List<Map.Entry<String, String>>,
    productDefinition: SingleReportProductDefinition,
  ): List<Prompt> =
    prompts.mapNotNull { entry ->
      mapToMatchingParameter(productDefinition, entry)
        ?.let { Prompt(entry.key, entry.value, it.filterType) }
    }

  private fun mapToMatchingParameter(
    productDefinition: SingleReportProductDefinition,
    entry: Map.Entry<String, String>,
  ) = productDefinition.reportDataset.parameters?.firstOrNull { parameter -> parameter.name == entry.key }

  private fun <K, V> toMap(entries: List<Map.Entry<K, V>>): Map<K, V> =
    entries.associate { it.toPair() }

  private fun partitionToPromptsAndFilters(
    filters: Map<String, String>,
    productDefinition: SingleReportProductDefinition,
  ) = filters.asIterable().partition { e -> isPrompt(productDefinition, e) }

  private fun isPrompt(
    productDefinition: SingleReportProductDefinition,
    e: Map.Entry<String, String>,
  ) = productDefinition.reportDataset.parameters?.any { it.name == e.key } ?: false

  private fun formatColumnsAndApplyFormulas(
    records: List<Map<String, Any?>>,
    schemaFields: List<SchemaField>,
    formulaEngine: FormulaEngine,
  ) = records
    .map { row -> formatColumnNamesToSourceFieldNamesCasing(row, schemaFields.map(SchemaField::name)) }
    .map(formulaEngine::applyFormulas)
}
