package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import kotlinx.coroutines.delay
import org.apache.commons.lang3.time.StopWatch
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Datasource
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementCancellationResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionStatus
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.TableIdGenerator

abstract class AthenaAndRedshiftCommonRepository(
  private val tableIdGenerator: TableIdGenerator,
) : RepositoryHelper() {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val END_STATES = listOf("FINISHED", "ABORTED", "FAILED")
  }

  abstract fun executeQueryAsync(
    productDefinition: SingleReportProductDefinition,
    filters: List<ConfiguredApiRepository.Filter>,
    sortColumn: String?,
    sortedAsc: Boolean,
    policyEngineResult: String,
    dynamicFilterFieldId: Set<String>? = null,
    prompts: Map<String, String>? = null,
    userToken: DprAuthAwareAuthenticationToken? = null,
  ): StatementExecutionResponse

  abstract fun getStatementStatus(statementId: String): StatementExecutionStatus

  abstract fun cancelStatementExecution(statementId: String): StatementCancellationResponse

  protected abstract fun buildSummaryQuery(query: String, summaryTableId: String): String

  protected abstract fun executeQueryAsync(
    datasource: Datasource,
    tableId: String,
    finalQuery: String,
  ): StatementExecutionResponse

  fun getPaginatedExternalTableResult(
    tableId: String,
    selectedPage: Long,
    pageSize: Long,
    jdbcTemplate: NamedParameterJdbcTemplate = populateJdbcTemplate(),
  ): List<Map<String, Any?>> {
    val stopwatch = StopWatch.createStarted()
    val result = jdbcTemplate
      .queryForList(
        "SELECT * FROM reports.$tableId limit $pageSize OFFSET ($selectedPage - 1) * $pageSize;",
        MapSqlParameterSource(),
      )
      .map {
        transformTimestampToLocalDateTime(it)
      }
    stopwatch.stop()
    log.debug("Query Execution time in ms: {}", stopwatch.time)
    return result
  }

  suspend fun createSummaryTable(
    datasource: Datasource,
    tableId: String,
    summaryId: String,
    dataset: Dataset,
  ) {
    val substitutedQuery = dataset.query.replace(TABLE_TOKEN_NAME, "reports.$tableId")
    val tableSummaryId = tableIdGenerator.getTableSummaryId(tableId, summaryId)
    val createTableQuery = buildSummaryQuery(
      substitutedQuery,
      tableSummaryId,
    )
    val stopwatch = StopWatch.createStarted()
    val executionResponse = executeQueryAsync(datasource, tableSummaryId, createTableQuery)

    waitForTableToBeCreated(executionResponse)

    stopwatch.stop()
    RepositoryHelper.log.debug("Create Summary Query Execution time in ms: {}", stopwatch.time)
  }

  private suspend fun waitForTableToBeCreated(executionResponse: StatementExecutionResponse) {
    var status: String
    do {
      delay(1000)
      status = getStatementStatus(executionResponse.executionId).status
    } while (!END_STATES.contains(status))
  }
}
