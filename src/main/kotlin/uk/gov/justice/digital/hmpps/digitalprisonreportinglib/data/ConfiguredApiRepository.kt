package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.apache.commons.lang3.time.StopWatch
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import java.sql.Timestamp

@Service
class ConfiguredApiRepository {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    const val EXTERNAL_MOVEMENTS_PRODUCT_ID = "external-movements"
    private const val STAGE_1 = """stage_1"""
    private const val STAGE_2 = """stage_2"""
    private const val STAGE_3 = """stage_3"""
  }

  @Autowired
  lateinit var jdbcTemplate: NamedParameterJdbcTemplate
  fun executeQuery(
    query: String,
    filters: List<Filter>,
    selectedPage: Long,
    pageSize: Long,
    sortColumn: String?,
    sortedAsc: Boolean,
    reportId: String,
    policyEngineResult: String,
    dynamicFilterFieldId: String? = null,
  ): List<Map<String, Any>> {
    val stopwatch = StopWatch.createStarted()
    val result = jdbcTemplate.queryForList(
      buildFinalQuery(
        buildReportQuery(query),
        buildPolicyQuery(policyEngineResult),
        buildFiltersQuery(filters),
        buildPaginationQuery(dynamicFilterFieldId, sortColumn, sortedAsc, pageSize, selectedPage),
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
  private fun buildReportQuery(query: String) = """WITH $STAGE_1 AS ($query)"""
  private fun buildPolicyQuery(policyEngineResult: String) = """$STAGE_2 AS (SELECT * FROM $STAGE_1 WHERE $policyEngineResult)"""
  private fun buildFiltersQuery(filters: List<Filter>) =
    """$STAGE_3 AS (SELECT * FROM $STAGE_2 WHERE ${buildFiltersWhereClause(filters)})"""
  private fun buildPaginationQuery(
    dynamicFilterFieldId: String?,
    sortColumn: String?,
    sortedAsc: Boolean,
    pageSize: Long,
    selectedPage: Long,
  ) = """SELECT ${constructProjectedColumns(dynamicFilterFieldId)}
        FROM stage_3 ${buildOrderByClause(sortColumn, sortedAsc)} 
        limit $pageSize OFFSET ($selectedPage - 1) * $pageSize;"""

  private fun buildOrderByClause(sortColumn: String?, sortedAsc: Boolean) =
    sortColumn?.let { """ORDER BY $sortColumn ${calculateSortingDirection(sortedAsc)}""" } ?: ""

  private fun buildFinalQuery(
    reportQuery: String,
    policiesQuery: String,
    filtersQuery: String,
    selectFromFinalStageQuery: String,
  ) = listOf(reportQuery, policiesQuery, filtersQuery).joinToString(",") + "\n$selectFromFinalStageQuery"

  private fun calculateSortingDirection(sortedAsc: Boolean): String {
    return if (sortedAsc) "asc" else "desc"
  }

  private fun constructProjectedColumns(dynamicFilterFieldId: String?) =
    dynamicFilterFieldId?.let { "DISTINCT $dynamicFilterFieldId" } ?: "*"

  fun count(
    filters: List<Filter>,
    query: String,
    reportId: String,
    policyEngineResult: String,
  ): Long {
    return jdbcTemplate.queryForList(
      buildFinalQuery(
        buildReportQuery(query),
        buildPolicyQuery(policyEngineResult),
        buildFiltersQuery(filters),
        "SELECT COUNT(1) as total FROM $STAGE_3",
      ),
      buildPreparedStatementNamedParams(filters),
    ).first()?.get("total") as Long
  }

  private fun transformTimestampToLocalDateTime(it: MutableMap<String, Any>) = it.entries.associate { (k, v) ->
    if (v is Timestamp) {
      k to v.toLocalDateTime()
    } else {
      k to v
    }
  }

  private fun buildFiltersWhereClause(
    filters: List<Filter>,
  ): String {
    return filters.joinToString(" AND ", transform = this::buildCondition).ifEmpty { "TRUE" }
  }

  fun buildPreparedStatementNamedParams(filters: List<Filter>): MapSqlParameterSource {
    val preparedStatementNamedParams = MapSqlParameterSource()
    filters.forEach { preparedStatementNamedParams.addValue(it.getKey(), it.value.lowercase()) }
    return preparedStatementNamedParams
  }

  private fun buildCondition(filter: Filter): String {
    val lowerCaseField = "lower(${filter.field})"
    val key = filter.getKey()

    return when (filter.type) {
      FilterType.STANDARD -> "$lowerCaseField = :$key"
      FilterType.RANGE_START -> "$lowerCaseField >= :$key"
      FilterType.DATE_RANGE_START -> "${filter.field} >= CAST(:$key AS timestamp)"
      FilterType.RANGE_END -> "$lowerCaseField <= :$key"
      FilterType.DATE_RANGE_END -> "${filter.field} < (CAST(:$key AS timestamp) + INTERVAL '1' day)"
      FilterType.DYNAMIC -> "${filter.field} LIKE '${filter.value}%'"
    }
  }

  data class Filter(
    val field: String,
    val value: String,
    val type: FilterType = FilterType.STANDARD,
  ) {
    fun getKey(): String = "${this.field}${this.type.suffix}".lowercase()
  }

  enum class FilterType(val suffix: String = "") {
    STANDARD,
    RANGE_START(".start"),
    RANGE_END(".end"),
    DATE_RANGE_START(".start"),
    DATE_RANGE_END(".end"),
    DYNAMIC,
  }
}
