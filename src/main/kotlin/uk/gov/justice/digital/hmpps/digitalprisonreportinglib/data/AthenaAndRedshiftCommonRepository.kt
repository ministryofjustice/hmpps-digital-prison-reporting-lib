package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.apache.commons.lang3.time.StopWatch
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Datasource
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementCancellationResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionStatus
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Prompt

abstract class AthenaAndRedshiftCommonRepository : RepositoryHelper() {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  abstract fun executeQueryAsync(
    productDefinition: SingleReportProductDefinition,
    filters: List<ConfiguredApiRepository.Filter>,
    sortColumn: String?,
    sortedAsc: Boolean,
    policyEngineResult: String,
    dynamicFilterFieldId: Set<String>? = null,
    prompts: List<Prompt>? = null,
    userToken: DprAuthAwareAuthenticationToken? = null,
  ): StatementExecutionResponse

  abstract fun getStatementStatus(statementId: String, tableId: String? = null): StatementExecutionStatus

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
    val result = jdbcTemplate
      .queryForList(
        "SELECT * FROM reports.$tableId WHERE $whereClause ${buildOrderByClause(sortColumn, sortedAsc)} LIMIT $pageSize OFFSET ($selectedPage - 1) * $pageSize;",
        MapSqlParameterSource(),
      )
      .map {
        transformTimestampToLocalDateTime(it)
      }
    stopwatch.stop()
    log.debug("Query Execution time in ms: {}", stopwatch.time)
    return result
  }

  fun isTablePresent(tableId: String, jdbcTemplate: NamedParameterJdbcTemplate = populateNamedParameterJdbcTemplate()): Boolean {
    val stopwatch = StopWatch.createStarted()
    val result = jdbcTemplate
      .queryForList(
        "SELECT tablename FROM SVV_EXTERNAL_TABLES WHERE schemaname = 'reports' AND tablename = '$tableId'",
        MapSqlParameterSource(),
      )
    stopwatch.stop()
    log.debug("Query Execution time in ms: {}", stopwatch.time)
    return !result.isNullOrEmpty()
  }
}
