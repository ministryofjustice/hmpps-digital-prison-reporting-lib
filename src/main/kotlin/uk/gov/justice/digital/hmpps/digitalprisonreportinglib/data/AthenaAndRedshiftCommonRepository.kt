package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.apache.commons.lang3.time.StopWatch
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Datasource
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.MultiphaseQuery
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportFilter
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportSummary
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.MultiphaseQueryExecution
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementCancellationResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionStatus
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Prompt
import java.util.Base64

abstract class AthenaAndRedshiftCommonRepository : RepositoryHelper() {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  abstract fun executeQueryAsync(
    filters: List<ConfiguredApiRepository.Filter>,
    sortColumn: String? = null,
    sortedAsc: Boolean,
    policyEngineResult: String,
    dynamicFilterFieldId: Set<String>? = null,
    prompts: List<Prompt>? = null,
    userToken: DprAuthAwareAuthenticationToken? = null,
    query: String,
    reportFilter: ReportFilter? = null,
    datasource: Datasource,
    reportSummaries: List<ReportSummary>? = null,
    allDatasets: List<Dataset>,
    productDefinitionId: String,
    productDefinitionName: String,
    reportOrDashboardId: String,
    reportOrDashboardName: String,
    preGeneratedDatasetTableId: String? = null,
    multiphaseQueries: List<MultiphaseQuery>? = null,
  ): StatementExecutionResponse

  abstract fun getStatementStatus(statementId: String): StatementExecutionStatus

  abstract fun cancelStatementExecution(statementId: String): StatementCancellationResponse

  abstract fun executeQueryAsync(
    datasource: Datasource,
    tableId: String,
    query: String,
  ): StatementExecutionResponse

  fun getPaginatedExternalTableResult(
    tableId: String,
    selectedPage: Long,
    pageSize: Long,
    filters: List<ConfiguredApiRepository.Filter>,
    sortedAsc: Boolean = false,
    sortColumn: String? = null,
    jdbcTemplate: NamedParameterJdbcTemplate = populateNamedParameterJdbcTemplate(),
  ): List<Map<String, Any?>> {
    val stopwatch = StopWatch.createStarted()
    val whereClause = buildFiltersWhereClause(filters)
    val query = "SELECT * FROM reports.$tableId WHERE $whereClause ${buildOrderByClause(sortColumn, sortedAsc) } LIMIT $pageSize OFFSET ($selectedPage - 1) * $pageSize;"
    log.debug("Query to get results: {}", query)
    val result = jdbcTemplate
      .queryForList(
        query,
        MapSqlParameterSource(),
      )
      .map {
        transformTimestampToLocalDateTime(it)
      }
    stopwatch.stop()
    log.debug("Query Execution time in ms: {}", stopwatch.time)
    return result
  }

  fun getDashboardPaginatedExternalTableResult(
    tableId: String,
    selectedPage: Long,
    pageSize: Long? = null,
    filters: List<ConfiguredApiRepository.Filter>,
    sortedAsc: Boolean = false,
    sortColumn: String? = null,
    jdbcTemplate: NamedParameterJdbcTemplate = populateNamedParameterJdbcTemplate(),
  ): List<Map<String, Any?>> {
    val stopwatch = StopWatch.createStarted()
    val whereClause = buildFiltersWhereClause(filters)
    val paginationSql = pageSize?.let { "LIMIT $pageSize OFFSET ($selectedPage - 1) * $pageSize" } ?: if (selectedPage == 1L) "" else return emptyList()
    val query = "SELECT * FROM reports.$tableId WHERE $whereClause ${buildOrderByClause(sortColumn, sortedAsc) } $paginationSql;"
    log.debug("Query to get results: {}", query)
    val result = jdbcTemplate
      .queryForList(
        query,
        MapSqlParameterSource(),
      )
      .map {
        transformTimestampToLocalDateTime(it)
      }
    stopwatch.stop()
    log.debug("Query Execution time in ms: {}", stopwatch.time)
    return result
  }

  fun isTableMissing(tableId: String, jdbcTemplate: NamedParameterJdbcTemplate = populateNamedParameterJdbcTemplate()): Boolean {
    val stopwatch = StopWatch.createStarted()
    val result = jdbcTemplate
      .queryForList(
        "SELECT tablename FROM SVV_EXTERNAL_TABLES WHERE schemaname = 'reports' AND tablename = '$tableId'",
        MapSqlParameterSource(),
      )
    stopwatch.stop()
    log.debug("Query Execution time in ms: {}", stopwatch.time)
    return result.isNullOrEmpty()
  }

  fun getStatementStatusForMultiphaseQuery(rootExecutionId: String, jdbcTemplate: NamedParameterJdbcTemplate = populateNamedParameterJdbcTemplate()): StatementExecutionStatus {
    val executions = getExecutions(rootExecutionId, jdbcTemplate)
    log.debug("All mapped QueryExecutions: {}", executions)
    return executions.firstOrNull { it.currentState == QUERY_FAILED }?.let {
      StatementExecutionStatus(
        status = QUERY_FAILED,
        duration = 1,
        resultRows = 0,
        resultSize = 0,
        error = it.error,
        stateChangeReason = it.error,
      )
    }
      ?: executions.firstOrNull { it.currentState == QUERY_CANCELLED }?.let {
        StatementExecutionStatus(
          status = QUERY_ABORTED,
          duration = 1,
          resultRows = 0,
          resultSize = 0,
        )
      }
      ?: StatementExecutionStatus(
        status = executions.maxByOrNull { it.index }!!.currentState?.let { mapAthenaStateToRedshiftState(it) } ?: QUERY_SUBMITTED,
        duration = 1,
        resultRows = 0,
        resultSize = 0,
      )
  }

  protected fun mapAthenaStateToRedshiftState(queryState: String): String {
    val athenaToRedshiftStateMappings = mapOf(
      QUERY_QUEUED to QUERY_SUBMITTED,
      QUERY_RUNNING to QUERY_STARTED,
      QUERY_SUCCEEDED to QUERY_FINISHED,
      QUERY_CANCELLED to QUERY_ABORTED,
    )
    return athenaToRedshiftStateMappings.getOrDefault(queryState, queryState)
  }

  private fun getExecutions(rootExecutionId: String, jdbcTemplate: NamedParameterJdbcTemplate): List<MultiphaseQueryExecution> {
    val stopwatch = StopWatch.createStarted()
    val mapSqlParameterSource = MapSqlParameterSource()
    log.debug("Retrieving query executions...")
    mapSqlParameterSource.addValue("rootExecutionId", rootExecutionId)
    val result = jdbcTemplate
      .queryForList(
        "SELECT $INDEX_COL, $CURRENT_STATE_COL, $ERROR_COL FROM admin.multiphase_query_state WHERE $ROOT_EXECUTION_ID_COL = :rootExecutionId;",
        mapSqlParameterSource,
      )
      .map {
        transformTimestampToLocalDateTime(it)
      }
    stopwatch.stop()
    log.debug("Query to get executions for root execution {} completed in {}ms", rootExecutionId, stopwatch.time)
    log.debug("All execution rows from the admin table: {}", result)
    return result.map {
      MultiphaseQueryExecution(
        index = it[INDEX_COL] as Int,
        currentState = it[CURRENT_STATE_COL] as String?,
        error = base64Decode(it[ERROR_COL] as String?),
      )
    }
  }

  private fun base64Decode(encoded: String?): String? = encoded?.let { String(Base64.getDecoder().decode(it.removeSurrounding("\"").toByteArray())) }
}
