package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.apache.commons.lang3.time.StopWatch
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementCancellationResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionStatus
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.TableIdGenerator

abstract class AthenaAndRedshiftCommonRepository(
  private val tableIdGenerator: TableIdGenerator,
  private val datasetHelper: DatasetHelper,
) : RepositoryHelper() {

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
    prompts: Map<String, String>? = null,
  ): StatementExecutionResponse

  abstract fun getStatementStatus(statementId: String): StatementExecutionStatus

  abstract fun cancelStatementExecution(statementId: String): StatementCancellationResponse

  protected abstract fun buildSummaryQuery(query: String, summaryTableId: String): String

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

  fun getFullExternalTableResult(
    tableId: String,
    jdbcTemplate: NamedParameterJdbcTemplate = populateJdbcTemplate(),
  ): List<Map<String, Any?>> {
    val stopwatch = StopWatch.createStarted()
    val result = jdbcTemplate
      .queryForList(
        "SELECT * FROM reports.$tableId;",
        MapSqlParameterSource(),
      )
      .map {
        transformTimestampToLocalDateTime(it)
      }
    stopwatch.stop()
    log.debug("Query Execution time in ms: {}", stopwatch.time)
    return result
  }

  fun count(tableId: String, jdbcTemplate: NamedParameterJdbcTemplate = populateJdbcTemplate()): Long {
    return jdbcTemplate.queryForList(
      "SELECT COUNT(1) as total FROM reports.$tableId;",
      MapSqlParameterSource(),
    ).first()?.get("total") as Long
  }

  protected fun buildSummaryQueries(productDefinition: SingleReportProductDefinition, tableId: String): String? {
    return productDefinition.report.summary?.joinToString(" ") {
      val summaryTableId = tableIdGenerator.getTableSummaryId(tableId, it.id)
      val query = datasetHelper.findDataset(productDefinition.allDatasets, it.dataset).query
      val substitutedQuery = query.replace(TABLE_TOKEN_NAME, "reports.$tableId")

      buildSummaryQuery(
        substitutedQuery,
        summaryTableId,
      )
    }
  }
}
