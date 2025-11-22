package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import jakarta.validation.ValidationException
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate
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
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.DatasourceConnection
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.MultiphaseQuery
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportFilter
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportSummary
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SqlDialect
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
const val QUERY_CANCELED = "CANCELED"
const val QUERY_RUNNING = "RUNNING"
const val QUERY_QUEUED = "QUEUED"
const val QUERY_SUBMITTED = "SUBMITTED"
const val ROOT_EXECUTION_ID_COL = "root_execution_id"
const val CURRENT_EXECUTION_ID_COL = "current_execution_id"
const val CURRENT_STATE_COL = "current_state"
const val DATASOURCE_NAME_COL = "datasource_name"
const val CATALOG_COL = "catalog"
const val DATABASE_COL = "database"
const val INDEX_COL = "index"
const val QUERY_COL = "query"
const val SEQUENCE_NUMBER_COL = "sequence_number"
const val LAST_UPDATE_COL = "last_update"
const val ERROR_COL = "error"

@Service
@Primary
@ConditionalOnBean(AthenaClient::class)
class AthenaApiRepository(
  val athenaClient: AthenaClient,
  val tableIdGenerator: TableIdGenerator,
  @Value("\${dpr.lib.redshiftdataapi.athenaworkgroup:workgroupArn}")
  val athenaWorkgroup: String,
  val jdbcTemplate: JdbcTemplate? = null,
) : AthenaAndRedshiftCommonRepository() {

  companion object {
    val tableIdRegex = "\\$\\{table\\[(\\d+)]}".toRegex()
  }

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
    multiphaseQueries: List<MultiphaseQuery>?,
  ): StatementExecutionResponse = multiphaseQueries?.takeIf { it.isNotEmpty() }?.let {
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

  private fun findTableIdOrThrow(indexToTableId: Map<Int, String>, index: Int): String = indexToTableId[index]
    ?: throw ValidationException("Invalid index. There is no table at index $index.")

  private fun buildSingleFinalQueryWithExternalTable(
    productDefinitionId: String,
    productDefinitionName: String,
    reportOrDashboardId: String,
    reportOrDashboardName: String,
    tableId: String,
    userToken: DprAuthAwareAuthenticationToken?,
    prompts: List<Prompt>?,
    query: String,
    reportFilter: ReportFilter?,
    policyEngineResult: String,
    dynamicFilterFieldId: Set<String>?,
    sortColumn: String?,
    sortedAsc: Boolean,
    datasource: Datasource,
  ) = buildCachedTableQuery(
    productDefinitionId = productDefinitionId,
    productDefinitionName = productDefinitionName,
    reportOrDashboardId = reportOrDashboardId,
    reportOrDashboardName = reportOrDashboardName,
    tableId = tableId,
    // For single non-multiphase queries we keep existing functionality to run as Nomis queries.
    connection = datasource.connection ?: DatasourceConnection.FEDERATED,
    innerQuery = buildFinalInnerQuery(
      buildContextQuery(userToken, datasource.dialect ?: SqlDialect.ORACLE11g),
      buildPromptsQuery(prompts, datasource.dialect ?: SqlDialect.ORACLE11g),
      buildDatasetQuery(query),
      buildReportQuery(reportFilter),
      buildPolicyQuery(policyEngineResult, determinePreviousCteName(reportFilter)),
      "$FILTER_ AS (SELECT * FROM $POLICY_ WHERE $TRUE_WHERE_CLAUSE)",
      buildFinalStageQuery(dynamicFilterFieldId, sortColumn, sortedAsc),
    ),
  )

  private fun buildContextQuery(userToken: DprAuthAwareAuthenticationToken?, dialect: SqlDialect? = null): String = """WITH $CONTEXT AS (
      SELECT 
      '${userToken?.getUsername()}' AS username, 
      '${userToken?.getActiveCaseLoadId()}' AS caseload, 
      'GENERAL' AS account_type 
      ${if (isOracleDialect(dialect)) "FROM DUAL" else ""}
      )"""

  private fun buildFinalInnerQuery(
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
    multiphaseQueries: List<MultiphaseQuery>,
  ): StatementExecutionResponse {
    val jdbcTemplate = jdbcTemplate ?: populateJdbcTemplate()
    if (multiphaseQueries.size == 1) {
      return executeSingleQueryAndInsertIntoMultiphaseTable(
        productDefinitionId,
        productDefinitionName,
        reportOrDashboardId,
        reportOrDashboardName,
        userToken,
        prompts,
        reportFilter,
        multiphaseQueries,
        policyEngineResult,
        sortColumn,
        sortedAsc,
        jdbcTemplate,
      )
    }
    val indexToTableId: Map<Int, String> = multiphaseQueries.associate { it.index to tableIdGenerator.generateNewExternalTableId() }
    val multiphaseQuerySortedByIndex = multiphaseQueries.sortedBy { it.index }
    val firstStatementExecutionResponse = buildExecuteAndInsertFirstQueryIntoMultiphaseTable(
      productDefinitionId,
      productDefinitionName,
      reportOrDashboardId,
      reportOrDashboardName,
      userToken,
      prompts,
      multiphaseQuerySortedByIndex,
      jdbcTemplate,
      indexToTableId,
    )
    buildAndInsertIntermediateQueryIntoMultiphaseTable(
      multiphaseQuerySortedByIndex,
      productDefinitionId,
      productDefinitionName,
      reportOrDashboardId,
      reportOrDashboardName,
      userToken,
      prompts,
      firstStatementExecutionResponse,
      jdbcTemplate,
      indexToTableId,
    )
    buildAndInsertLastQueryIntoMultiphaseTable(
      productDefinitionId,
      productDefinitionName,
      reportOrDashboardId,
      reportOrDashboardName,
      userToken,
      prompts,
      multiphaseQuerySortedByIndex,
      reportFilter,
      policyEngineResult,
      sortColumn,
      sortedAsc,
      firstStatementExecutionResponse,
      jdbcTemplate,
      indexToTableId,
    )
    return StatementExecutionResponse(
      tableId = findTableIdOrThrow(indexToTableId, multiphaseQuerySortedByIndex.last().index),
      executionId = firstStatementExecutionResponse.executionId,
    )
  }

  private fun buildAndInsertLastQueryIntoMultiphaseTable(
    productDefinitionId: String,
    productDefinitionName: String,
    reportOrDashboardId: String,
    reportOrDashboardName: String,
    userToken: DprAuthAwareAuthenticationToken?,
    prompts: List<Prompt>?,
    multiphaseQuerySortedByIndex: List<MultiphaseQuery>,
    reportFilter: ReportFilter?,
    policyEngineResult: String,
    sortColumn: String?,
    sortedAsc: Boolean,
    firstStatementExecutionResponse: StatementExecutionResponse,
    jdbcTemplate: JdbcTemplate,
    indexToTableId: Map<Int, String>,
  ) {
    val lastQuery = buildCachedTableQuery(
      productDefinitionId = productDefinitionId,
      productDefinitionName = productDefinitionName,
      reportOrDashboardId = reportOrDashboardId,
      reportOrDashboardName = reportOrDashboardName,
      tableId = findTableIdOrThrow(indexToTableId, multiphaseQuerySortedByIndex.last().index),
      connection = multiphaseQuerySortedByIndex.last().datasource.connection ?: throwNoConnectionDefinedException(multiphaseQuerySortedByIndex.last().index),
      innerQuery = buildFinalInnerQuery(
        buildContextQuery(userToken, multiphaseQuerySortedByIndex.last().datasource.dialect ?: SqlDialect.ATHENA3),
        buildPromptsQuery(prompts, multiphaseQuerySortedByIndex.last().datasource.dialect ?: SqlDialect.ATHENA3),
        buildDatasetQuery(interpolateQuery(multiphaseQuerySortedByIndex.last().query, indexToTableId)),
        buildReportQuery(reportFilter),
        buildPolicyQuery(policyEngineResult, determinePreviousCteName(reportFilter)),
        "$FILTER_ AS (SELECT * FROM $POLICY_ WHERE $TRUE_WHERE_CLAUSE)",
        buildFinalStageQuery(sortColumn = sortColumn, sortedAsc = sortedAsc),
      ),
    )
    log.debug("Last multiphase query: {}", lastQuery)
    val lastInsertStatement = buildInsertStatement(
      rootExecutionId = firstStatementExecutionResponse.executionId,
      datasourceName = multiphaseQuerySortedByIndex.last().datasource.name,
      datasourceCatalog = multiphaseQuerySortedByIndex.last().datasource.catalog,
      datasourceDatabase = multiphaseQuerySortedByIndex.last().datasource.database,
      index = multiphaseQuerySortedByIndex.last().index,
      query = lastQuery,
    )
    log.debug("Inserting into admin table: {}", lastInsertStatement)
    jdbcTemplate.execute(lastInsertStatement)
  }

  private fun throwNoConnectionDefinedException(index: Int): Nothing = throw ValidationException("Query at index $index has no connection defined in its datasource.")

  private fun buildAndInsertIntermediateQueryIntoMultiphaseTable(
    multiphaseQuerySortedByIndex: List<MultiphaseQuery>,
    productDefinitionId: String,
    productDefinitionName: String,
    reportOrDashboardId: String,
    reportOrDashboardName: String,
    userToken: DprAuthAwareAuthenticationToken?,
    prompts: List<Prompt>?,
    firstStatementExecutionResponse: StatementExecutionResponse,
    jdbcTemplate: JdbcTemplate,
    indexToTableId: Map<Int, String>,
  ) {
    List(
      multiphaseQuerySortedByIndex
        .drop(1)
        .dropLast(1).size,
    ) { i ->
      val intermediateQuery = multiphaseQuerySortedByIndex[i + 1]
      val intermediateQueryString = buildCachedTableQuery(
        productDefinitionId = productDefinitionId,
        productDefinitionName = productDefinitionName,
        reportOrDashboardId = reportOrDashboardId,
        reportOrDashboardName = reportOrDashboardName,
        tableId = findTableIdOrThrow(indexToTableId, intermediateQuery.index),
        // For every subsequent query apart from the first connection is required.
        connection = intermediateQuery.datasource.connection ?: throwNoConnectionDefinedException(intermediateQuery.index),
        innerQuery = (
          listOf(
            buildContextQuery(userToken, intermediateQuery.datasource.dialect ?: SqlDialect.ATHENA3),
            buildPromptsQuery(prompts, intermediateQuery.datasource.dialect ?: SqlDialect.ATHENA3),
            buildDatasetQuery(interpolateQuery(intermediateQuery.query, indexToTableId)),
          )
            .joinToString(",") +
            "\nSELECT * FROM $DATASET_"
          ),
      )
      log.debug("Intermediate query at index ${i + 1}: {}", intermediateQueryString)
      val insertQuery = buildInsertStatement(
        rootExecutionId = firstStatementExecutionResponse.executionId,
        datasourceName = intermediateQuery.datasource.name,
        datasourceCatalog = intermediateQuery.datasource.catalog,
        datasourceDatabase = intermediateQuery.datasource.database,
        index = intermediateQuery.index,
        query = intermediateQueryString,
      )
      log.debug("Inserting into admin table: {}", insertQuery)
      jdbcTemplate.execute(insertQuery)
    }
  }

  private fun interpolateQuery(
    query: String,
    indexToTableId: Map<Int, String>,
  ): String = tableIdRegex.replace(query) { matchResult ->
    findTableIdOrThrow(indexToTableId, matchResult.groupValues[1].toInt())
  }

  private fun buildExecuteAndInsertFirstQueryIntoMultiphaseTable(
    productDefinitionId: String,
    productDefinitionName: String,
    reportOrDashboardId: String,
    reportOrDashboardName: String,
    userToken: DprAuthAwareAuthenticationToken?,
    prompts: List<Prompt>?,
    multiphaseQuerySortedByIndex: List<MultiphaseQuery>,
    jdbcTemplate: JdbcTemplate,
    indexToTableId: Map<Int, String>,
  ): StatementExecutionResponse {
    val tableId = findTableIdOrThrow(indexToTableId, multiphaseQuerySortedByIndex[0].index)
    val firstQuery = buildCachedTableQuery(
      productDefinitionId = productDefinitionId,
      productDefinitionName = productDefinitionName,
      reportOrDashboardId = reportOrDashboardId,
      reportOrDashboardName = reportOrDashboardName,
      tableId = tableId,
      // Default the first multiphase query to federated mainly for backward compatibility with existing Nomis queries.
      connection = multiphaseQuerySortedByIndex[0].datasource.connection ?: DatasourceConnection.FEDERATED,
      innerQuery = (
        listOf(
          buildContextQuery(userToken, multiphaseQuerySortedByIndex[0].datasource.dialect ?: SqlDialect.ORACLE11g),
          buildPromptsQuery(prompts, multiphaseQuerySortedByIndex[0].datasource.dialect ?: SqlDialect.ORACLE11g),
          buildDatasetQuery(multiphaseQuerySortedByIndex[0].query),
        )
          .joinToString(",") +
          "\nSELECT * FROM $DATASET_"
        ),
    )

    log.debug("Database query at index ${multiphaseQuerySortedByIndex[0].index}: $firstQuery")
    val firstQueryDatasource = multiphaseQuerySortedByIndex[0].datasource
    val statementExecutionResponse = executeQueryAsync(firstQueryDatasource, tableId, firstQuery)
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
    return statementExecutionResponse
  }

  private fun executeSingleQueryAndInsertIntoMultiphaseTable(
    productDefinitionId: String,
    productDefinitionName: String,
    reportOrDashboardId: String,
    reportOrDashboardName: String,
    userToken: DprAuthAwareAuthenticationToken?,
    prompts: List<Prompt>?,
    reportFilter: ReportFilter?,
    multiphaseQueries: List<MultiphaseQuery>,
    policyEngineResult: String,
    sortColumn: String?,
    sortedAsc: Boolean,
    jdbcTemplate: JdbcTemplate,
  ): StatementExecutionResponse {
    val tableId = tableIdGenerator.generateNewExternalTableId()
    val singleFinalQuery = buildSingleFinalQueryWithExternalTable(
      productDefinitionId = productDefinitionId,
      productDefinitionName = productDefinitionName,
      reportOrDashboardId = reportOrDashboardId,
      reportOrDashboardName = reportOrDashboardName,
      tableId = tableId,
      userToken = userToken,
      prompts = prompts,
      reportFilter = reportFilter,
      query = multiphaseQueries.first().query,
      policyEngineResult = policyEngineResult,
      sortColumn = sortColumn,
      sortedAsc = sortedAsc,
      dynamicFilterFieldId = null,
      datasource = multiphaseQueries.first().datasource,
    )
    val singleQueryExecutionResult =
      executeQueryAsync(multiphaseQueries.first().datasource, tableId, singleFinalQuery)
    val insertStatement: String =
      buildInsertStatement(
        rootExecutionId = singleQueryExecutionResult.executionId,
        currentExecutionId = singleQueryExecutionResult.executionId,
        datasourceName = multiphaseQueries.first().datasource.name,
        datasourceCatalog = multiphaseQueries.first().datasource.catalog,
        datasourceDatabase = multiphaseQueries.first().datasource.database,
        index = multiphaseQueries.first().index,
        query = singleFinalQuery,
      )
    log.debug("Inserting into admin table: {}", insertStatement)
    jdbcTemplate.execute(insertStatement)
    return singleQueryExecutionResult
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
          admin.multiphase_query_state (
          $ROOT_EXECUTION_ID_COL,
          ${currentExecutionId?.let { "$CURRENT_EXECUTION_ID_COL,"} ?: ""}
          $DATASOURCE_NAME_COL,
          ${datasourceCatalog?.let { "$CATALOG_COL,"} ?: "" }
          ${datasourceDatabase?.let { "$DATABASE_COL," } ?: "" }
          $INDEX_COL,
          $QUERY_COL,
          $SEQUENCE_NUMBER_COL,
          $LAST_UPDATE_COL
          )
          values (
            '$rootExecutionId',
            ${currentExecutionId?.let { "'$it'," } ?: ""}
            '$datasourceName',
            ${datasourceCatalog?.let { "'$it'," } ?: ""}
            ${datasourceDatabase?.let { "'$it'," } ?: ""}
            $index,
            '${Base64.getEncoder().encodeToString(query.toByteArray())}',
            0,
            SYSDATE
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
    val finalQuery = buildSingleFinalQueryWithExternalTable(
      productDefinitionId,
      productDefinitionName,
      reportOrDashboardId,
      reportOrDashboardName,
      tableId,
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

    return executeQueryAsync(datasource, tableId, finalQuery)
  }

  private fun buildCachedTableQuery(
    productDefinitionId: String,
    productDefinitionName: String,
    reportOrDashboardId: String,
    reportOrDashboardName: String,
    tableId: String,
    connection: DatasourceConnection,
    innerQuery: String,
  ): String {
    val fullQuery =
      when (connection) {
        DatasourceConnection.FEDERATED ->
          """
          /* $productDefinitionId $productDefinitionName $reportOrDashboardId $reportOrDashboardName */
          CREATE TABLE AwsDataCatalog.reports.$tableId 
          WITH (
            format = 'PARQUET'
          ) 
          AS (
          SELECT * FROM TABLE(system.query(query =>
           '${innerQuery.replace("'", "''")}'
           )) 
          );
          """.trimIndent()

        DatasourceConnection.AWS_DATA_CATALOG ->
          """
            /* $productDefinitionId $productDefinitionName $reportOrDashboardId $reportOrDashboardName */
                CREATE TABLE AwsDataCatalog.reports.$tableId
                WITH (
                  format = 'PARQUET'
                ) 
                AS (
          $innerQuery
                )
          """.trimIndent()

        else -> throw RuntimeException("Unsupported DatasourceConnection type for query execution. Connection: $connection")
      }
    log.debug("Full query is: {}", fullQuery)
    return fullQuery
  }
}
