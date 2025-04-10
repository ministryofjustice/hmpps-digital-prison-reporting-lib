package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.AthenaError
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.QueryExecutionContext
import software.amazon.awssdk.services.athena.model.QueryExecutionStatus
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.StopQueryExecutionRequest
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Datasource
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterType.Date
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.MultiphaseQuery
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportFilter
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportSummary
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementCancellationResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionStatus
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.TableIdGenerator
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Prompt
import java.util.Base64

const val QUERY_STARTED = "STARTED"
const val QUERY_FINISHED = "FINISHED"
const val QUERY_ABORTED = "ABORTED"
const val QUERY_FAILED = "FAILED"
const val QUERY_SUCCEEDED = "SUCCEEDED"
const val QUERY_CANCELLED = "CANCELLED"
const val QUERY_RUNNING = "RUNNING"
const val QUERY_QUEUED = "QUEUED"
const val QUERY_SUBMITTED = "SUBMITTED"

@Service
@Primary
@ConditionalOnBean(AthenaClient::class)
class AthenaApiRepository(
  val athenaClient: AthenaClient,
  val tableIdGenerator: TableIdGenerator,
  @Value("\${dpr.lib.redshiftdataapi.athenaworkgroup:workgroupArn}")
  val athenaWorkgroup: String,
) : AthenaAndRedshiftCommonRepository() {

  override fun executeQueryAsync(
    filters: List<ConfiguredApiRepository.Filter>,
    sortColumn: String?,
    sortedAsc: Boolean,
    policyEngineResult: String,
    dynamicFilterFieldId: Set<String>?,
    prompts: List<Prompt>?,
    userToken: DprAuthAwareAuthenticationToken?,
    query: String,
    reportFilter: ReportFilter?,
    datasource: Datasource,
    reportSummaries: List<ReportSummary>?,
    allDatasets: List<Dataset>,
    productDefinitionId: String,
    productDefinitionName: String,
    reportOrDashboardId: String,
    reportOrDashboardName: String,
    preGeneratedDatasetTableId: String?,
    multiphaseQuery: List<MultiphaseQuery>?,
  ): StatementExecutionResponse = multiphaseQuery?.takeIf { it.isNotEmpty() }?.let {
    executeMultiphaseQuery(
      productDefinitionId,
      productDefinitionName,
      reportOrDashboardId,
      reportOrDashboardName,
      userToken,
      prompts,
      reportFilter,
      policyEngineResult,
      sortColumn,
      sortedAsc,
      datasource,
      it,
    )
  }
    ?: executeSingleQuery(
      productDefinitionId,
      productDefinitionName,
      reportOrDashboardId,
      reportOrDashboardName,
      userToken,
      prompts,
      query,
      reportFilter,
      policyEngineResult,
      dynamicFilterFieldId,
      sortColumn,
      sortedAsc,
      datasource,
    )

  private fun executeMultiphaseQuery(
    productDefinitionId: String,
    productDefinitionName: String,
    reportOrDashboardId: String,
    reportOrDashboardName: String,
    userToken: DprAuthAwareAuthenticationToken?,
    prompts: List<Prompt>?,
    reportFilter: ReportFilter?,
    policyEngineResult: String,
    sortColumn: String?,
    sortedAsc: Boolean,
    datasource: Datasource,
    multiphaseQuery: List<MultiphaseQuery>,
  ): StatementExecutionResponse {
    val multiphaseQuerySortedByIndex = multiphaseQuery.sortedBy { it.index }
    val firstTableId = tableIdGenerator.generateNewExternalTableId()
    val firstQuery = """
            /* $productDefinitionId $productDefinitionName $reportOrDashboardId $reportOrDashboardName */
            CREATE TABLE AwsDataCatalog.reports.$firstTableId 
            WITH (
              format = 'PARQUET'
            ) 
            AS (
            SELECT * FROM TABLE(system.query(query =>
             '${(
      listOf(buildContextQuery(userToken), buildPromptsQuery(prompts), buildDatasetQuery(multiphaseQuerySortedByIndex[0].query))
        .joinToString(",") +
        "\nSELECT * FROM $DATASET_"
      ).replace("'", "''")}'
             )) 
            );
    """.trimIndent()
    log.debug("Database query at index ${multiphaseQuerySortedByIndex[0].index}: $firstQuery")
    val firstQueryDatasource = multiphaseQuerySortedByIndex[0].datasource
    val statementExecutionResponse = executeQueryAsync(firstQueryDatasource, firstTableId, firstQuery)
    val jdbcTemplate = populateJdbcTemplate(firstQueryDatasource.name)
    val insertStatement: String =
      buildInsertStatement(
        rootExecutionId = statementExecutionResponse.executionId,
        currentExecutionId = statementExecutionResponse.executionId,
        datasourceName = firstQueryDatasource.name,
        datasourceCatalog = firstQueryDatasource.catalog,
        datasourceDatabase = firstQueryDatasource.database,
        index = multiphaseQuerySortedByIndex[0].index,
        query = firstQuery,
      )
    log.debug("Inserting into admin table: {}", insertStatement)
    jdbcTemplate.execute(insertStatement)
    var previousTableId = firstTableId
    List(
      multiphaseQuerySortedByIndex
        .drop(1)
        .dropLast(1).size,
    ) { i ->
      val currentTableId = tableIdGenerator.generateNewExternalTableId()
      val intermediateQuery = multiphaseQuerySortedByIndex[i + 1]
      val intermediateQueryString = """
          /* $productDefinitionId $productDefinitionName $reportOrDashboardId $reportOrDashboardName */
              CREATE TABLE AwsDataCatalog.reports.$currentTableId
              WITH (
                format = 'PARQUET'
              ) 
              AS (
              ${listOf(buildContextQuery(userToken, false), buildPromptsQuery(prompts, false), buildDatasetQuery(intermediateQuery.query))
        .joinToString(",") +
        "\nSELECT * FROM $DATASET_"
          .replace("\${tableId}", previousTableId)
      }
              )
      """.trimIndent()
      log.debug("Intermediate query at index ${i + 1}: {}", intermediateQueryString)
      val insertQuery = buildInsertStatement(
        rootExecutionId = statementExecutionResponse.executionId,
        datasourceName = intermediateQuery.datasource.name,
        datasourceCatalog = intermediateQuery.datasource.catalog,
        datasourceDatabase = intermediateQuery.datasource.database,
        index = intermediateQuery.index,
        query = intermediateQueryString,
      )
      log.debug("Inserting into admin table: {}", insertQuery)
      jdbcTemplate.execute(insertQuery)
      previousTableId = currentTableId
    }
    val lastTableId = tableIdGenerator.generateNewExternalTableId()
    val lastQuery = """
      /* $productDefinitionId $productDefinitionName $reportOrDashboardId $reportOrDashboardName */
              CREATE TABLE AwsDataCatalog.reports.$lastTableId
              WITH (
                format = 'PARQUET'
              ) 
              AS (
               ${buildFinalQuery(
      buildContextQuery(userToken, false),
      buildPromptsQuery(prompts, false),
      buildDatasetQuery(multiphaseQuerySortedByIndex.last().query),
      buildReportQuery(reportFilter),
      buildPolicyQuery(policyEngineResult, determinePreviousCteName(reportFilter)),
      "$FILTER_ AS (SELECT * FROM $POLICY_ WHERE $TRUE_WHERE_CLAUSE)",
      buildFinalStageQuery(sortColumn = sortColumn, sortedAsc = sortedAsc),
    )
      .replace("\${tableId}", previousTableId)}
              )
    """.trimIndent()
    log.debug("Last multiphase query: {}", lastQuery)
    val lastInsertStatement = buildInsertStatement(
      rootExecutionId = statementExecutionResponse.executionId,
      datasourceName = multiphaseQuerySortedByIndex.last().datasource.name,
      datasourceCatalog = multiphaseQuerySortedByIndex.last().datasource.catalog,
      datasourceDatabase = multiphaseQuerySortedByIndex.last().datasource.database,
      index = multiphaseQuerySortedByIndex.last().index,
      query = lastQuery,
    )
    log.debug("Inserting into admin table: {}", lastInsertStatement)
    jdbcTemplate.execute(lastInsertStatement)
    return StatementExecutionResponse(lastTableId, statementExecutionResponse.executionId)
  }

  private fun buildInsertStatement(
    rootExecutionId: String,
    currentExecutionId: String? = null,
    datasourceName: String,
    datasourceCatalog: String? = null,
    datasourceDatabase: String? = null,
    index: Int,
    query: String,
  ) = """insert into 
          admin.execution_manager (
          root_execution_id,
          ${currentExecutionId?.let { "current_execution_id,"} ?: ""}
          datasource,
          ${datasourceCatalog?.let { "catalog,"} ?: "" }
          ${datasourceDatabase?.let { "database," } ?: "" }
          index,
          query
          )
          values (
            '$rootExecutionId',
            ${currentExecutionId?.let { "'$it'," } ?: ""}
            '$datasourceName',
            ${datasourceCatalog?.let { "'$it'," } ?: ""}
            ${datasourceDatabase?.let { "'$it'," } ?: ""}
            $index,
            '${Base64.getEncoder().encodeToString(query.toByteArray())}'
          )
  """.trimMargin()

  private fun executeSingleQuery(
    productDefinitionId: String,
    productDefinitionName: String,
    reportOrDashboardId: String,
    reportOrDashboardName: String,
    userToken: DprAuthAwareAuthenticationToken?,
    prompts: List<Prompt>?,
    query: String,
    reportFilter: ReportFilter?,
    policyEngineResult: String,
    dynamicFilterFieldId: Set<String>?,
    sortColumn: String?,
    sortedAsc: Boolean,
    datasource: Datasource,
  ): StatementExecutionResponse {
    val tableId = tableIdGenerator.generateNewExternalTableId()
    val finalQuery = """
          /* $productDefinitionId $productDefinitionName $reportOrDashboardId $reportOrDashboardName */
          CREATE TABLE AwsDataCatalog.reports.$tableId 
          WITH (
            format = 'PARQUET'
          ) 
          AS (
          SELECT * FROM TABLE(system.query(query =>
           '${
      buildFinalQuery(
        buildContextQuery(userToken),
        buildPromptsQuery(prompts),
        buildDatasetQuery(query),
        buildReportQuery(reportFilter),
        buildPolicyQuery(policyEngineResult, determinePreviousCteName(reportFilter)),
        // The filters part will be replaced with the variables CTE
        "$FILTER_ AS (SELECT * FROM $POLICY_ WHERE $TRUE_WHERE_CLAUSE)",
        buildFinalStageQuery(dynamicFilterFieldId, sortColumn, sortedAsc),
      ).replace("'", "''")
    }'
           )) 
          );
    """.trimIndent()

    return executeQueryAsync(datasource, tableId, finalQuery)
  }

  override fun executeQueryAsync(
    datasource: Datasource,
    tableId: String,
    query: String,
  ): StatementExecutionResponse {
    val queryExecutionContext = QueryExecutionContext.builder()
      .database(datasource.database)
      .catalog(datasource.catalog)
      .build()
    val startQueryExecutionRequest = StartQueryExecutionRequest.builder()
      .queryString(query)
      .queryExecutionContext(queryExecutionContext)
      .workGroup(athenaWorkgroup)
      .build()
    log.debug("Full async query: {}", query)
    val queryExecutionId = athenaClient
      .startQueryExecution(startQueryExecutionRequest).queryExecutionId()
    return StatementExecutionResponse(tableId, queryExecutionId)
  }

  override fun getStatementStatus(statementId: String): StatementExecutionStatus {
    val getQueryExecutionRequest = GetQueryExecutionRequest.builder()
      .queryExecutionId(statementId)
      .build()

    val getQueryExecutionResponse = athenaClient.getQueryExecution(getQueryExecutionRequest)
    val status = getQueryExecutionResponse.queryExecution().status()
    val stateChangeReason = status.stateChangeReason()
    val error: AthenaError? = status.athenaError()
    return StatementExecutionStatus(
      status = mapAthenaStateToRedshiftState(status.state().toString()),
      duration = calculateDuration(status),
      resultRows = 0,
      resultSize = 0,
      error = error?.errorMessage(),
      errorCategory = error?.errorCategory(),
      stateChangeReason = stateChangeReason,
    )
  }

  override fun cancelStatementExecution(statementId: String): StatementCancellationResponse {
    val cancelQueryExecutionRequest = StopQueryExecutionRequest.builder()
      .queryExecutionId(statementId)
      .build()

    athenaClient.stopQueryExecution(cancelQueryExecutionRequest)
    return StatementCancellationResponse(true)
  }

  override fun buildDatasetQuery(query: String) = if (query.contains("$DATASET_ AS", ignoreCase = true)) query else """$DATASET_ AS ($query)"""

  private fun buildPromptsQuery(prompts: List<Prompt>?, useDualTable: Boolean = true): String {
    if (prompts.isNullOrEmpty()) {
      return "$PROMPT AS (SELECT '' ${if (useDualTable) "FROM DUAL" else ""})"
    }
    val promptsCte = prompts.joinToString(", ") { prompt -> buildPromptsQueryBasedOnType(prompt) }
    return "$PROMPT AS (SELECT $promptsCte ${if (useDualTable) "FROM DUAL" else ""})"
  }

  private fun buildPromptsQueryBasedOnType(prompt: Prompt): String = when (prompt.type) {
    Date -> "TO_DATE('${prompt.value}','yyyy-mm-dd') AS ${prompt.name}"
    else -> "'${prompt.value}' AS ${prompt.name}"
  }

  private fun buildContextQuery(userToken: DprAuthAwareAuthenticationToken?, useDualTable: Boolean = true): String = """WITH $CONTEXT AS (
      SELECT 
      '${userToken?.getUsername()}' AS username, 
      '${userToken?.getActiveCaseLoadId()}' AS caseload, 
      'GENERAL' AS account_type 
      ${if (useDualTable) "FROM DUAL" else ""}
      )"""

  private fun buildFinalQuery(
    context: String,
    prompts: String,
    datasetQuery: String,
    reportQuery: String,
    policiesQuery: String,
    filtersQuery: String,
    selectFromFinalStageQuery: String,
  ): String {
    val query = listOf(context, prompts, datasetQuery, reportQuery, policiesQuery, filtersQuery).joinToString(",") + "\n$selectFromFinalStageQuery"
    log.debug("Database query: $query")
    return query
  }

  private fun calculateDuration(status: QueryExecutionStatus): Long = status.completionDateTime()?.let { completion ->
    status.submissionDateTime()?.let { submission ->
      completion.minusMillis(submission.toEpochMilli()).toEpochMilli() * 1000000
    } ?: 0
  } ?: 0
}
