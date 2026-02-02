package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.Count
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.AthenaAndRedshiftCommonRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.AthenaApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.IdentifiedHelper
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.QUERY_FINISHED
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RedshiftDataApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Identified.Companion.REF_PREFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.MultiphaseQuery
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SchemaField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.WithPolicy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementCancellationResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionStatus
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.MissingTableException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.UserAuthorisationException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.DownloadContext
import java.io.Writer
import java.sql.ResultSet
import java.util.Base64

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
  val s3ApiService: S3ApiService,
  @Value("\${URL_ENV_SUFFIX:#{null}}") val env: String? = null,
) : CommonDataApiService(identifiedHelper) {

  companion object {
    const val INVALID_FILTERS_MESSAGE = "Invalid filters provided."
    const val INVALID_STATIC_OPTIONS_MESSAGE = "Invalid static options provided."
    const val INVALID_DYNAMIC_OPTIONS_MESSAGE = "Invalid dynamic options length provided."
    const val INVALID_DYNAMIC_FILTER_MESSAGE = "Error. This filter is not a dynamic filter."
    const val MISSING_MANDATORY_FILTER_MESSAGE = "Mandatory filter value not provided:"
    const val FILTER_VALUE_DOES_NOT_MATCH_PATTERN_MESSAGE = "Filter value does not match pattern:"
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  private val datasourceNameToRepo: Map<String, AthenaAndRedshiftCommonRepository>
    get() = mapOf(
      "datamart" to redshiftDataApiRepository,
    )

  fun validateAndExecuteStatementAsync(
    reportId: String,
    reportVariantId: String,
    filters: Map<String, String>,
    sortColumn: String?,
    sortedAsc: Boolean?,
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
    val (promptsMap, filtersOnly) = partitionToPromptsAndFilters(
      filters,
      extractParameters(productDefinition.reportDataset, productDefinition.reportDataset.multiphaseQuery),
    )
    val preGeneratedTableId = checkForScheduledDataset(productDefinition)
    val (sortColumn, computedSortedAsc) = sortColumnFromQueryOrGetDefault(productDefinition, sortColumn, sortedAsc)
    return getRepo(productDefinition.datasource.name)
      .executeQueryAsync(
        filters = validateAndMapFilters(productDefinition, toMap(filtersOnly), false) + dynamicFilter,
        sortColumn,
        computedSortedAsc,
        policyEngineResult = policyEngine.execute(),
        dynamicFilterFieldId = reportFieldId,
        prompts = buildPrompts(
          productDefinition.reportDataset.multiphaseQuery,
          promptsMap,
          productDefinition.reportDataset.parameters,
        ),
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
        multiphaseQueries = productDefinition.reportDataset.multiphaseQuery,
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
    val (promptsMap, filtersOnly) = partitionToPromptsAndFilters(
      filters,
      extractParameters(productDefinition.dashboardDataset, productDefinition.dashboardDataset.multiphaseQuery),
    )
    log.debug("All filters from user are: {}", filters)
    log.debug("Prompts are: {}", promptsMap)
    log.debug("Filters only are: {}", filtersOnly)
    return getRepo(productDefinition.datasource.name)
      .executeQueryAsync(
        filters = validateAndMapFilters(productDefinition, toMap(filtersOnly), false),
        sortedAsc = true,
        policyEngineResult = policyEngine.execute(),
        prompts = buildPrompts(
          productDefinition.dashboardDataset.multiphaseQuery,
          promptsMap,
          productDefinition.dashboardDataset.parameters,
        ),
        userToken = userToken,
        query = productDefinition.dashboardDataset.query,
        reportFilter = productDefinition.dashboard.filter,
        datasource = productDefinition.datasource,
        allDatasets = productDefinition.allDatasets,
        productDefinitionId = productDefinition.id,
        productDefinitionName = productDefinition.name,
        reportOrDashboardId = productDefinition.dashboard.id,
        reportOrDashboardName = productDefinition.dashboard.name,
        multiphaseQueries = productDefinition.dashboardDataset.multiphaseQuery,
      )
  }

  fun getStatementStatus(statementId: String, reportId: String, reportVariantId: String, userToken: DprAuthAwareAuthenticationToken?, dataProductDefinitionsPath: String? = null, tableId: String? = null): StatementExecutionStatus {
    val productDefinition = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId, dataProductDefinitionsPath)
    checkAuth(productDefinition, userToken)
    return getStatementExecutionStatus(
      productDefinition.reportDataset.multiphaseQuery,
      productDefinition.datasource.name,
      statementId,
      tableId,
    )
  }

  fun getDashboardStatementStatus(statementId: String, productDefinitionId: String, dashboardId: String, userToken: DprAuthAwareAuthenticationToken?, dataProductDefinitionsPath: String? = null, tableId: String? = null): StatementExecutionStatus {
    val productDefinition = productDefinitionRepository.getSingleDashboardProductDefinition(productDefinitionId, dashboardId, dataProductDefinitionsPath)
    checkAuth(productDefinition, userToken)
    return getStatementExecutionStatus(
      productDefinition.dashboardDataset.multiphaseQuery,
      productDefinition.datasource.name,
      statementId,
      tableId,
    )
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

  fun prepareDownloadContext(
    reportId: String,
    reportVariantId: String,
    dataProductDefinitionsPath: String?,
    filters: Map<String, String>,
    selectedColumns: List<String>?,
    sortColumn: String?,
    sortedAsc: Boolean?,
    userToken: DprAuthAwareAuthenticationToken?,
  ): DownloadContext {
    val productDefinition = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId, dataProductDefinitionsPath)
    checkAuth(productDefinition, userToken)
    val formulaEngine = FormulaEngine(productDefinition.report.specification?.field ?: emptyList(), env, identifiedHelper)
    val (computedSortColumn, computedSortedAsc) = sortColumnFromQueryOrGetDefault(productDefinition, sortColumn, sortedAsc)
    val columnsTrimmed = selectedColumns?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet()?.takeIf { it.isNotEmpty() }
    validateColumns(productDefinition, columnsTrimmed)
    return DownloadContext(
      schemaFields = productDefinition.reportDataset.schema.field,
      reportFields = productDefinition.report.specification?.field,
      validatedFilters = validateAndMapFilters(productDefinition, filters, true),
      formulaEngine = formulaEngine,
      sortedAsc = computedSortedAsc,
      sortColumn = computedSortColumn,
      selectedAndValidatedColumns = columnsTrimmed,
    )
  }

  fun downloadCsv(
    writer: Writer,
    tableId: String,
    downloadContext: DownloadContext,
  ) {
    var allColumnsFormattedAndValidated: List<String>? = null
    lateinit var csvOutputColumns: List<String>
    redshiftDataApiRepository.streamExternalTableResult(
      tableId = tableId,
      filters = downloadContext.validatedFilters,
      sortedAsc = downloadContext.sortedAsc,
      sortColumn = downloadContext.sortColumn,
      rowConsumer = { rs ->
        if (allColumnsFormattedAndValidated == null) {
          allColumnsFormattedAndValidated = formatColumnNamesToSourceFieldNamesCasing(
            columnHeaders = extractColumnNames(rs),
            fieldNames = downloadContext.schemaFields.map(SchemaField::name),
          )
          csvOutputColumns = filterAndSortColumns(downloadContext.selectedAndValidatedColumns, allColumnsFormattedAndValidated)
          writeCsvHeader(
            writer = writer,
            columns = formatColumnsToDisplayNames(csvOutputColumns, downloadContext.reportFields, downloadContext.schemaFields),
          )
        }
        writeRowWithFormulaAsCsv(
          rs = rs,
          writer = writer,
          formulaEngine = downloadContext.formulaEngine,
          allColumns = allColumnsFormattedAndValidated,
          csvOutputColumns = csvOutputColumns,
        )
      },
    )
    writer.flush()
  }

  fun formatColumnsToDisplayNames(columnNames: List<String>, reportFields: List<ReportField>?, schemaFields: List<SchemaField>): List<String> = mapAllColumnNamesToDisplayFields(reportFields, schemaFields)
    .let { columnNameToDisplayMap ->
      columnNames.map { columnName -> columnNameToDisplayMap[columnName] ?: columnName }
    }

  private fun mapAllColumnNamesToDisplayFields(
    reportFields: List<ReportField>?,
    schemaFields: List<SchemaField>,
  ): Map<String, String> = schemaFields.associate { schemaField ->
    schemaField.name to calculateDisplayField(reportFields, schemaField)
  }

  private fun calculateDisplayField(
    reportFields: List<ReportField>?,
    schemaField: SchemaField,
  ): String = matchingReportField(reportFields, schemaField)?.display?.ifBlank { schemaField.display } ?: schemaField.display

  private fun matchingReportField(
    reportFields: List<ReportField>?,
    schemaField: SchemaField,
  ): ReportField? = reportFields?.firstOrNull { it.name.removePrefix(REF_PREFIX) == (schemaField.name) }

  private fun filterAndSortColumns(
    selectedAndValidatedColumns: Set<String>? = null,
    allColumnsFormattedAndValidated: List<String>,
  ): List<String> = selectedAndValidatedColumns?.takeIf { it.isNotEmpty() }?.let { selected ->
    selected.filter { it in allColumnsFormattedAndValidated }
  } ?: allColumnsFormattedAndValidated

  private fun validateColumns(
    productDefinition: SingleReportProductDefinition,
    columns: Set<String>? = null,
  ) {
    val specFieldNames =
      productDefinition.report.specification
        ?.field
        ?.map { it.name }
        ?.map { it.removePrefix(REF_PREFIX) }
        ?.toSet()
        ?: emptySet()
    val schemaFieldNames = productDefinition.reportDataset.schema.field.map { it.name }.toSet()
    columns?.takeIf { it.isNotEmpty() }?.let { cols ->
      val invalidSchemaColumns = cols.filterNot { it in schemaFieldNames }
      if (invalidSchemaColumns.isNotEmpty()) {
        throw IllegalArgumentException("Invalid columns, not in schema: $invalidSchemaColumns")
      }
      val invalidSpecColumns = cols.filterNot { it in specFieldNames }
      if (invalidSpecColumns.isNotEmpty()) {
        throw IllegalArgumentException("Invalid columns, not in report specification: $invalidSpecColumns")
      }
    }
  }

  private fun writeCsvHeader(
    writer: Writer,
    columns: List<String>,
  ) {
    writer.write(
      columns.joinToString(",") { escapeCsv(it) },
    )
    writer.write("\n")
  }

  private fun extractColumnNames(rs: ResultSet): List<String> {
    val meta = rs.metaData
    return (1..meta.columnCount).map { meta.getColumnLabel(it) }
  }

  private fun writeRowWithFormulaAsCsv(
    rs: ResultSet,
    writer: Writer,
    formulaEngine: FormulaEngine,
    allColumns: List<String>,
    csvOutputColumns: List<String>,
  ) {
    val fullRowWithFormulasApplied = formulaEngine.applyFormulas(buildRowWithAllColumns(allColumns, rs))
    csvOutputColumns.forEachIndexed { index, col ->
      if (index > 0) writer.write(",")
      writer.write(escapeCsv(fullRowWithFormulasApplied[col]))
    }
    writer.write("\n")
  }

  private fun escapeCsv(value: Any?): String {
    if (value == null) return ""

    val str = value.toString()
    val needsEscaping = str.contains(",") || str.contains("\"") || str.contains("\n")

    return if (needsEscaping) {
      "\"${str.replace("\"", "\"\"")}\""
    } else {
      str
    }
  }

  private fun buildRowWithAllColumns(columnNames: List<String>, rs: ResultSet): MutableMap<String, Any?> {
    val row = mutableMapOf<String, Any?>()

    columnNames.forEachIndexed { index, name ->
      row[name] = rs.getObject(index + 1)
    }
    return row
  }

  fun getStatementResult(
    tableId: String,
    reportId: String,
    reportVariantId: String,
    dataProductDefinitionsPath: String? = null,
    selectedPage: Long,
    pageSize: Long,
    filters: Map<String, String>,
    sortedAsc: Boolean?,
    sortColumn: String? = null,
    userToken: DprAuthAwareAuthenticationToken?,
  ): List<Map<String, Any?>> {
    val productDefinition = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId, dataProductDefinitionsPath)
    checkAuth(productDefinition, userToken)
    val formulaEngine = FormulaEngine(productDefinition.report.specification?.field ?: emptyList(), env, identifiedHelper)
    val (sortColumn, computedSortedAsc) = sortColumnFromQueryOrGetDefault(productDefinition, sortColumn, sortedAsc)
    return formatColumnsAndApplyFormulas(
      redshiftDataApiRepository.getPaginatedExternalTableResult(
        tableId,
        selectedPage,
        pageSize,
        validateAndMapFilters(productDefinition, filters, true),
        sortedAsc = computedSortedAsc,
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
    pageSize: Long? = null,
    filters: Map<String, String>,
    userToken: DprAuthAwareAuthenticationToken?,
  ): List<List<Map<String, Any?>>> {
    val productDefinition = productDefinitionRepository.getSingleDashboardProductDefinition(reportId, dashboardId, dataProductDefinitionsPath)
    checkAuth(productDefinition, userToken)
    return listOf(
      redshiftDataApiRepository.getDashboardPaginatedExternalTableResult(
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

  fun getSummaryResult(
    tableId: String,
    summaryId: String,
    reportId: String,
    reportVariantId: String,
    dataProductDefinitionsPath: String? = null,
    filters: Map<String, String>,
    userToken: DprAuthAwareAuthenticationToken?,
  ): List<Map<String, Any?>>? {
    val productDefinition = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId, dataProductDefinitionsPath)
    checkAuth(productDefinition, userToken)
    val summary = productDefinition.report.summary?.find { it.id == summaryId }
      ?: throw ValidationException("Invalid summary ID: $summaryId")

    val dataset = identifiedHelper.findOrFail(productDefinition.allDatasets, summary.dataset)
    val tableSummaryId = tableIdGenerator.getTableSummaryId(tableId, summaryId)

    val results = checkDataExistsAndFetch(tableSummaryId, tableId, summaryId, dataset, productDefinition)
    if (results == null) {
      return null
    }

    return results.map {
      formatColumnNamesToSourceFieldNamesCasing(it, dataset.schema.field.map(SchemaField::name))
    }
  }

  // Request data from the summary table.
  // If it doesn't exist, create it (waiting for creation to complete).
  // TODO: When looking at the interactive journey, we will need to figure out how to re-request the summaries when the filters have changed.
  fun checkDataExistsAndFetch(tableSummaryId: String, tableId: String, summaryId: String, dataset: Dataset, productDefinition: SingleReportProductDefinition): List<Map<String, Any?>>? {
    val tableExists = !redshiftDataApiRepository.isTableMissing(tableSummaryId)
    val s3DataExists = s3ApiService.doesObjectExist(tableSummaryId)

    if (tableExists && s3DataExists) {
      return redshiftDataApiRepository.getFullExternalTableResult(tableSummaryId)
    }
    if (!tableExists && !s3DataExists) {
      configuredApiRepository.createSummaryTable(tableId, summaryId, dataset.query, productDefinition.datasource.name)
      return redshiftDataApiRepository.getFullExternalTableResult(tableSummaryId)
    }
    // This is an error state really, and we only get here because the summary table creation is happening as part of this GET and because the cleanup for the tables and S3 happen independently
    // We will refactor this code so that summary tables get created, like redshift, as part of the main report generation.
    log.warn("Summary table in an inconsistent state.")
    return null
  }

  fun cancelStatementExecution(statementId: String, reportId: String, reportVariantId: String, userToken: DprAuthAwareAuthenticationToken?, dataProductDefinitionsPath: String? = null): StatementCancellationResponse {
    val productDefinition = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId, dataProductDefinitionsPath)
    checkAuth(productDefinition, userToken)
    return getRepo(productDefinition.datasource.name).cancelStatementExecution(statementId)
  }

  fun cancelDashboardStatementExecution(statementId: String, definitionId: String, dashboardId: String, userToken: DprAuthAwareAuthenticationToken?, dataProductDefinitionsPath: String? = null): StatementCancellationResponse {
    val productDefinition = productDefinitionRepository.getSingleDashboardProductDefinition(definitionId, dashboardId, dataProductDefinitionsPath)
    checkAuth(productDefinition, userToken)
    return getRepo(productDefinition.datasource.name).cancelStatementExecution(statementId)
  }

  fun count(tableId: String): Count = Count(redshiftDataApiRepository.count(tableId))

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
    productDefinition: SingleReportProductDefinition,
  ): String? {
    val generatedTableId = generateScheduledDatasetId(productDefinition)
    // check if dataset configured for scheduling and table exists
    return if (productDefinition.hasDatasetScheduled() && !redshiftDataApiRepository.isTableMissing(generatedTableId.lowercase())) {
      // generate external table id
      generatedTableId
    } else {
      null
    }
  }

  fun SingleReportProductDefinition.hasDatasetScheduled(): Boolean {
    val reportScheduled = this.scheduled ?: false
    return reportScheduled && this.reportDataset.schedule != null
  }

  fun generateScheduledDatasetId(definition: SingleReportProductDefinition): String {
    val id = "${definition.id}:${definition.reportDataset.id}"
    val encodedId = Base64.getEncoder().encodeToString(id.toByteArray())
    val updatedId = encodedId.replace("=", "_")
    return "_$updatedId"
  }

  private fun getStatementExecutionStatus(
    multiphaseQuery: List<MultiphaseQuery>?,
    datasourceName: String,
    statementId: String,
    tableId: String?,
  ): StatementExecutionStatus = getStatusOrThrowIfTableIsMissing(tableId, calculateStatementStatus(multiphaseQuery, datasourceName, statementId))

  private fun calculateStatementStatus(
    multiphaseQuery: List<MultiphaseQuery>?,
    datasourceName: String,
    statementId: String,
  ): StatementExecutionStatus = multiphaseQuery?.takeIf { it.isNotEmpty() }?.let {
    redshiftDataApiRepository.getStatementStatusForMultiphaseQuery(statementId)
  } ?: getRepo(datasourceName).getStatementStatus(statementId)

  private fun getStatusOrThrowIfTableIsMissing(
    tableId: String?,
    statementStatus: StatementExecutionStatus,
  ): StatementExecutionStatus {
    tableId?.takeIf { statementStatus.status == QUERY_FINISHED }?.let {
      if (redshiftDataApiRepository.isTableMissing(tableId)) {
        throw MissingTableException(tableId)
      }
    }
    return statementStatus
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

  private fun getRepo(datasourceName: String): AthenaAndRedshiftCommonRepository = datasourceNameToRepo.getOrDefault(datasourceName.lowercase(), athenaApiRepository)

  private fun <K, V> toMap(entries: List<Map.Entry<K, V>>): Map<K, V> = entries.associate { it.toPair() }

  private fun formatColumnsAndApplyFormulas(
    records: List<Map<String, Any?>>,
    schemaFields: List<SchemaField>,
    formulaEngine: FormulaEngine,
  ) = records
    .map { row -> formatColumnNamesToSourceFieldNamesCasing(row, schemaFields.map(SchemaField::name)) }
    .map(formulaEngine::applyFormulas)
}
