package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.apache.commons.lang3.time.StopWatch
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportFilter
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SqlDialect
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Prompt

@Service
class ConfiguredApiRepository(
  private val redShiftSummaryTableHelper: RedShiftSummaryTableHelper,
) : RepositoryHelper() {

  fun executeQuery(
    query: String,
    filters: List<Filter>,
    selectedPage: Long,
    pageSize: Long,
    sortColumn: String?,
    sortedAsc: Boolean,
    policyEngineResult: String,
    dynamicFilterFieldId: Set<String>? = null,
    dataSourceName: String,
    reportFilter: ReportFilter? = null,
    prompts: List<Prompt>? = null,
  ): List<Map<String, Any?>> {
    val stopwatch = StopWatch.createStarted()
    val jdbcTemplate = populateNamedParameterJdbcTemplate(dataSourceName)
    // The result of the query can contain null values.
    // This is coming from Java and if the returned type is not specified in Kotlin it will assume it is List<Map<String, Any>>
    // while in reality it is List<Map<String, Any?>>.
    val result: List<Map<String, Any?>> = jdbcTemplate.queryForList(
      determineFinalQuery(
        prompts = prompts,
        query = query,
        policyEngineResult = policyEngineResult,
        filters = filters,
        selectedPage = selectedPage,
        pageSize = pageSize,
        sortColumn = sortColumn,
        sortedAsc = sortedAsc,
        dynamicFilterFieldId = dynamicFilterFieldId,
        reportFilter = reportFilter,
      ),
      buildPreparedStatementNamedParams(filters),
    )
      .map {
        transformTimestampToLocalDateTime(it)
      }
    stopwatch.stop()
    log.debug("Query Execution time in ms: {}", stopwatch.time)
    return result
  }

  private fun determineFinalQuery(
    prompts: List<Prompt>?,
    query: String,
    policyEngineResult: String,
    filters: List<Filter>,
    selectedPage: Long,
    pageSize: Long,
    sortColumn: String?,
    sortedAsc: Boolean,
    dynamicFilterFieldId: Set<String>?,
    reportFilter: ReportFilter?,
  ): String = prompts?.takeIf { it.isNotEmpty() }?.let {
    buildFinalQuery(
      prompts = buildPromptsQuery(it, SqlDialect.REDSHIFT4),
      datasetQuery = buildDatasetQuery(query),
      reportQuery = buildReportQuery(reportFilter),
      policiesQuery = buildPolicyQuery(policyEngineResult, determinePreviousCteName(reportFilter)),
      filtersQuery = buildFiltersQuery(filters),
      selectFromFinalStageQuery = buildFinalStageQueryWithPagination(
        dynamicFilterFieldId,
        sortColumn,
        sortedAsc,
        pageSize,
        selectedPage,
      ),
    ) + ";"
  } ?: (
    buildFinalQuery(
      datasetQuery = super.buildDatasetQuery(query),
      reportQuery = buildReportQuery(reportFilter),
      policiesQuery = buildPolicyQuery(policyEngineResult, determinePreviousCteName(reportFilter)),
      filtersQuery = buildFiltersQuery(filters),
      selectFromFinalStageQuery = buildFinalStageQueryWithPagination(
        dynamicFilterFieldId,
        sortColumn,
        sortedAsc,
        pageSize,
        selectedPage,
      ),
    ) + ";"
    )

  override fun buildDatasetQuery(query: String) = """$DATASET_ AS ($query)"""

  private fun buildFinalQuery(
    prompts: String,
    datasetQuery: String,
    reportQuery: String,
    policiesQuery: String,
    filtersQuery: String,
    selectFromFinalStageQuery: String,
  ): String {
    val query = listOf(prompts, datasetQuery, reportQuery, policiesQuery, filtersQuery).joinToString(",") + "\n$selectFromFinalStageQuery"
    log.debug("Database query: $query")
    return query
  }

  private fun buildFinalStageQueryWithPagination(
    dynamicFilterFieldId: Set<String>?,
    sortColumn: String?,
    sortedAsc: Boolean,
    pageSize: Long,
    selectedPage: Long,
  ) = """${buildFinalStageQuery(dynamicFilterFieldId, sortColumn, sortedAsc)} 
        ${buildPaginationQuery(pageSize, selectedPage)}"""

  private fun buildPaginationQuery(pageSize: Long, selectedPage: Long) = """limit $pageSize OFFSET ($selectedPage - 1) * $pageSize"""

  fun count(
    filters: List<Filter>,
    query: String,
    reportId: String,
    policyEngineResult: String,
    dataSourceName: String,
    productDefinition: SingleReportProductDefinition,
  ): Long {
    val jdbcTemplate = populateNamedParameterJdbcTemplate(dataSourceName)
    return jdbcTemplate.queryForList(
      buildFinalQuery(
        datasetQuery = super.buildDatasetQuery(query),
        reportQuery = buildReportQuery(productDefinition.report.filter),
        policiesQuery = buildPolicyQuery(policyEngineResult),
        filtersQuery = buildFiltersQuery(filters),
        selectFromFinalStageQuery = "SELECT COUNT(1) as total FROM $FILTER_",
      ) + ";",
      buildPreparedStatementNamedParams(filters),
    ).first()?.get("total") as Long
  }

  private fun buildPreparedStatementNamedParams(filters: List<Filter>): MapSqlParameterSource {
    val preparedStatementNamedParams = MapSqlParameterSource()
    filters
      .filterNot { it.type == FilterType.DYNAMIC }
      .filterNot { it.type == FilterType.BOOLEAN }
      .filterNot { it.type == FilterType.MULTISELECT }
      .forEach { preparedStatementNamedParams.addValue(it.getKey(), it.value.lowercase()) }
    filters.filter { it.type == FilterType.BOOLEAN }.forEach { preparedStatementNamedParams.addValue(it.getKey(), it.value.toBoolean()) }
    addNamedParamsForMultiselect(filters, preparedStatementNamedParams)

    log.debug("Prepared statement named parameters: {}", preparedStatementNamedParams)
    return preparedStatementNamedParams
  }

  private fun addNamedParamsForMultiselect(
    filters: List<Filter>,
    preparedStatementNamedParams: MapSqlParameterSource,
  ) {
    filters.filter { it.type == FilterType.MULTISELECT }
      .forEach { filter ->
        filter.value.split(",")
          .forEachIndexed { i, v ->
            preparedStatementNamedParams.addValue(filter.field + i, v)
          }
      }
  }

  fun createSummaryTable(tableId: String, summaryId: String, query: String, dataSourceName: String) {
    val jdbcTemplate = populateJdbcTemplate(dataSourceName)
    val createTableQuery = redShiftSummaryTableHelper.buildSummaryQuery(query, tableId, summaryId)
    jdbcTemplate.execute(createTableQuery)
  }

  data class Filter(
    val field: String,
    val value: String,
    val type: FilterType = FilterType.STANDARD,
  ) {
    fun getKey(): String = "${this.field}${this.type.suffix}".lowercase()
  }
}
