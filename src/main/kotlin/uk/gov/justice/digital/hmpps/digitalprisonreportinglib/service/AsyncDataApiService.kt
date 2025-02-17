package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import jakarta.validation.ValidationException
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.jdbc.UncategorizedSQLException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.Count
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.MetricData
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.AthenaAndRedshiftCommonRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.AthenaApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.IdentifiedHelper
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.QUERY_FINISHED
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RedshiftDataApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Parameter
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SchemaField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.WithPolicy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementCancellationResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionStatus
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.MissingTableException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.UserAuthorisationException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Prompt
import java.util.*

@Service
@ConditionalOnBean(value = [RedshiftDataApiRepository::class, AthenaApiRepository::class])
class AsyncDataApiService(
  val productDefinitionRepository: ProductDefinitionRepository,
  val configuredApiRepository: ConfiguredApiRepository,
  val redshiftDataApiRepository: RedshiftDataApiRepository,
  val athenaApiRepository: AthenaApiRepository,
  val tableIdGenerator: TableIdGenerator,
  identifiedHelper: IdentifiedHelper,
  val productDefinitionTokenPolicyChecker: ProductDefinitionTokenPolicyChecker,
  @Value("\${URL_ENV_SUFFIX:#{null}}") val env: String? = null,
) : CommonDataApiService(identifiedHelper) {

  companion object {
    const val INVALID_FILTERS_MESSAGE = "Invalid filters provided."
    const val INVALID_STATIC_OPTIONS_MESSAGE = "Invalid static options provided."
    const val INVALID_DYNAMIC_OPTIONS_MESSAGE = "Invalid dynamic options length provided."
    const val INVALID_DYNAMIC_FILTER_MESSAGE = "Error. This filter is not a dynamic filter."
    const val MISSING_MANDATORY_FILTER_MESSAGE = "Mandatory filter value not provided:"
    const val FILTER_VALUE_DOES_NOT_MATCH_PATTERN_MESSAGE = "Filter value does not match pattern:"
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
    checkAuth(productDefinition, userToken)
    val dynamicFilter = buildAndValidateDynamicFilter(reportFieldId?.first(), prefix, productDefinition)
    val policyEngine = PolicyEngine(productDefinition.policy, userToken)
    val (promptsMap, filtersOnly) = partitionToPromptsAndFilters(filters, productDefinition.reportDataset.parameters)
    val preGeneratedTableId = checkForScheduledDataset(productDefinition)
    return getRepo(productDefinition.datasource.name)
      .executeQueryAsync(
        filters = validateAndMapFilters(productDefinition, toMap(filtersOnly), false) + dynamicFilter,
        sortColumn = sortColumnFromQueryOrGetDefault(productDefinition, sortColumn),
        sortedAsc = sortedAsc,
        policyEngineResult = policyEngine.execute(),
        dynamicFilterFieldId = reportFieldId,
        prompts = buildPrompts(promptsMap, productDefinition.reportDataset.parameters),
        userToken = userToken,
        query = productDefinition.reportDataset.query,
        reportFilter = productDefinition.report.filter,
        datasource = productDefinition.datasource,
        reportSummaries = productDefinition.report.summary,
        allDatasets = productDefinition.allDatasets,
        productDefinitionId = productDefinition.id,
        productDefinitionName = productDefinition.name,
        reportOrDashboardId = productDefinition.report.id,
        reportOrDashboardName = productDefinition.report.name,
        preGeneratedDatasetTableId = preGeneratedTableId,
      )
  }

  fun validateAndExecuteStatementAsync(
    reportId: String,
    dashboardId: String,
    userToken: DprAuthAwareAuthenticationToken?,
    dataProductDefinitionsPath: String? = null,
    filters: Map<String, String>,
  ): StatementExecutionResponse {
    val productDefinition = productDefinitionRepository.getSingleDashboardProductDefinition(
      definitionId = reportId,
      dashboardId = dashboardId,
      dataProductDefinitionsPath = dataProductDefinitionsPath,
    )
    checkAuth(productDefinition, userToken)
    val policyEngine = PolicyEngine(productDefinition.policy, userToken)
    val (promptsMap, filtersOnly) = partitionToPromptsAndFilters(filters, productDefinition.dashboardDataset.parameters)
    return getRepo(productDefinition.datasource.name)
      .executeQueryAsync(
        filters = validateAndMapFilters(productDefinition, toMap(filtersOnly), false),
        sortedAsc = true,
        policyEngineResult = policyEngine.execute(),
        prompts = buildPrompts(promptsMap, productDefinition.dashboardDataset.parameters),
        userToken = userToken,
        query = productDefinition.dashboardDataset.query,
        reportFilter = productDefinition.dashboard.filter,
        datasource = productDefinition.datasource,
        allDatasets = productDefinition.allDatasets,
        productDefinitionId = productDefinition.id,
        productDefinitionName = productDefinition.name,
        reportOrDashboardId = productDefinition.dashboard.id,
        reportOrDashboardName = productDefinition.dashboard.name,
      )
  }

  fun getStatementStatus(statementId: String, reportId: String, reportVariantId: String, userToken: DprAuthAwareAuthenticationToken?, dataProductDefinitionsPath: String? = null, tableId: String? = null): StatementExecutionStatus {
    val productDefinition = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId, dataProductDefinitionsPath)
    checkAuth(productDefinition, userToken)
    val statementStatus = getRepo(productDefinition.datasource.name).getStatementStatus(statementId)
    tableId?.takeIf { statementStatus.status == QUERY_FINISHED }?.let {
      if (redshiftDataApiRepository.isTableMissing(tableId)) {
        throw MissingTableException(tableId)
      }
    }
    return statementStatus
  }

  fun getStatementStatus(statementId: String, tableId: String? = null): StatementExecutionStatus {
    val statementStatus = redshiftDataApiRepository.getStatementStatus(statementId)
    tableId?.takeIf { statementStatus.status == QUERY_FINISHED }?.let {
      if (redshiftDataApiRepository.isTableMissing(tableId)) {
        throw MissingTableException(tableId)
      }
    }
    return statementStatus
  }

  fun getStatementResult(
    tableId: String,
    reportId: String,
    reportVariantId: String,
    dataProductDefinitionsPath: String? = null,
    selectedPage: Long,
    pageSize: Long,
    filters: Map<String, String>,
    sortedAsc: Boolean,
    sortColumn: String? = null,
    userToken: DprAuthAwareAuthenticationToken?,
  ): List<Map<String, Any?>> {
    val productDefinition = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId, dataProductDefinitionsPath)
    checkAuth(productDefinition, userToken)
    val formulaEngine = FormulaEngine(productDefinition.report.specification?.field ?: emptyList(), env, identifiedHelper)
    return formatColumnsAndApplyFormulas(
      redshiftDataApiRepository.getPaginatedExternalTableResult(
        tableId,
        selectedPage,
        pageSize,
        validateAndMapFilters(productDefinition, filters, true),
        sortedAsc = sortedAsc,
        sortColumn = sortColumn,
      ),
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
    filters: Map<String, String>,
    userToken: DprAuthAwareAuthenticationToken?,
  ): List<List<Map<String, Any?>>> {
    val productDefinition = productDefinitionRepository.getSingleDashboardProductDefinition(reportId, dashboardId, dataProductDefinitionsPath)
    checkAuth(productDefinition, userToken)
    return listOf(
      redshiftDataApiRepository.getPaginatedExternalTableResult(
        tableId = tableId,
        selectedPage = selectedPage,
        pageSize = pageSize,
        filters = validateAndMapFilters(productDefinition, filters, true),
      )
        .map { row ->
          formatColumnNamesToSourceFieldNamesCasing(
            row,
            productDefinition.dashboardDataset.schema.field.map(SchemaField::name),
          )
        }
        .map { row -> toMetricData(row) },
    )
  }

  private fun toMetricData(row: Map<String, Any?>): Map<String, MetricData> =
    row.entries.associate { e -> e.key to MetricData(e.value) }

  fun getSummaryResult(
    tableId: String,
    summaryId: String,
    reportId: String,
    reportVariantId: String,
    dataProductDefinitionsPath: String? = null,
    filters: Map<String, String>,
    userToken: DprAuthAwareAuthenticationToken?,
  ): List<Map<String, Any?>> {
    val productDefinition = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId, dataProductDefinitionsPath)
    checkAuth(productDefinition, userToken)
    val summary = productDefinition.report.summary?.find { it.id == summaryId }
      ?: throw ValidationException("Invalid summary ID: $summaryId")

    val dataset = identifiedHelper.findOrFail(productDefinition.allDatasets, summary.dataset)
    val tableSummaryId = tableIdGenerator.getTableSummaryId(tableId, summaryId)

    // Request data from the summary table.
    // If it doesn't exist, create it (waiting for creation to complete).
    // TODO: When looking at the interactive journey, we will need to figure out how to re-request the summaries when the filters have changed.
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

  fun cancelStatementExecution(statementId: String, reportId: String, reportVariantId: String, userToken: DprAuthAwareAuthenticationToken?, dataProductDefinitionsPath: String? = null): StatementCancellationResponse {
    val productDefinition = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId, dataProductDefinitionsPath)
    checkAuth(productDefinition, userToken)
    return getRepo(productDefinition.datasource.name).cancelStatementExecution(statementId)
  }

  fun cancelStatementExecution(statementId: String): StatementCancellationResponse {
    return redshiftDataApiRepository.cancelStatementExecution(statementId)
  }

  fun count(tableId: String): Count {
    return Count(redshiftDataApiRepository.count(tableId))
  }

  fun count(tableId: String, reportId: String, reportVariantId: String, filters: Map<String, String>, userToken: DprAuthAwareAuthenticationToken?, dataProductDefinitionsPath: String? = null): Count {
    val productDefinition = productDefinitionRepository.getSingleReportProductDefinition(
      reportId,
      reportVariantId,
      dataProductDefinitionsPath,
    )
    checkAuth(productDefinition, userToken)
    return Count(redshiftDataApiRepository.count(tableId, validateAndMapFilters(productDefinition, filters, true)))
  }

  fun checkForScheduledDataset(
    productDefinition: SingleReportProductDefinition
  ): String? {

    val generatedTableId = generateScheduledDatasetId(productDefinition)
    //check if dataset configured for scheduling and table exists
    return if (productDefinition.hasDatasetScheduled() && !getRepo(productDefinition.datasource.name).isTableMissing(generatedTableId.lowercase())) {
      //generate external table id
      generatedTableId
    } else null
  }

  fun SingleReportProductDefinition.hasDatasetScheduled(): Boolean {
    val reportScheduled = this.scheduled ?: false
    return reportScheduled && this.reportDataset.schedule != null
  }

  fun generateScheduledDatasetId(definition: SingleReportProductDefinition) : String {
    val id = "${definition.id}:${definition.reportDataset.id}"
    val encodedId = Base64.getEncoder().encodeToString(id .toByteArray())
    val updatedId = encodedId.replace("=", "_")
    return "_${updatedId}"
  }

  private fun checkAuth(
    productDefinition: WithPolicy,
    userToken: DprAuthAwareAuthenticationToken?,
  ): Boolean {
    if (!productDefinitionTokenPolicyChecker.determineAuth(productDefinition, userToken)) {
      throw UserAuthorisationException("User does not have correct authorisation")
    }
    return true
  }

  private fun getRepo(datasourceName: String): AthenaAndRedshiftCommonRepository =
    datasourceNameToRepo.getOrDefault(datasourceName.lowercase(), redshiftDataApiRepository)

  private fun buildPrompts(
    prompts: List<Map.Entry<String, String>>,
    parameters: List<Parameter>?,
  ): List<Prompt> =
    prompts.mapNotNull { entry ->
      mapToMatchingParameter(entry, parameters)
        ?.let { Prompt(entry.key, entry.value, it.filterType) }
    }

  private fun mapToMatchingParameter(
    entry: Map.Entry<String, String>,
    parameters: List<Parameter>?,
  ) = parameters?.firstOrNull { parameter -> parameter.name == entry.key }

  private fun <K, V> toMap(entries: List<Map.Entry<K, V>>): Map<K, V> =
    entries.associate { it.toPair() }

  private fun partitionToPromptsAndFilters(
    filters: Map<String, String>,
    parameters: List<Parameter>?,
  ) = filters.asIterable().partition { e -> isPrompt(e, parameters) }

  private fun isPrompt(
    e: Map.Entry<String, String>,
    parameters: List<Parameter>?,
  ) = parameters?.any { it.name == e.key } ?: false

  private fun formatColumnsAndApplyFormulas(
    records: List<Map<String, Any?>>,
    schemaFields: List<SchemaField>,
    formulaEngine: FormulaEngine,
  ) = records
    .map { row -> formatColumnNamesToSourceFieldNamesCasing(row, schemaFields.map(SchemaField::name)) }
    .map(formulaEngine::applyFormulas)


}
