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
  }

  @Autowired
  lateinit var jdbcTemplate: NamedParameterJdbcTemplate
  fun executeQuery(
    query: String,
    filters: List<Filter>,
    selectedPage: Long,
    pageSize: Long,
    sortColumn: String,
    sortedAsc: Boolean,
    userCaseloads: List<String>,
    caseloadFields: List<String>,
    reportId: String,
  ): List<Map<String, Any>> {
    if (userCaseloads.isEmpty()) {
      log.warn("Zero records returned as the user has no active caseloads.")
      return emptyList()
    }
    val (preparedStatementNamedParams, whereClause) = buildWhereClause(filters, userCaseloads, caseloadFields, reportId)
    val sortingDirection = if (sortedAsc) "asc" else "desc"
    val stopwatch = StopWatch.createStarted()
    val result = jdbcTemplate.queryForList(
      buildQuery(query, whereClause, sortColumn, sortingDirection, pageSize, selectedPage),
      preparedStatementNamedParams,
    )
      .map {
        transformTimestampToLocalDateTime(it)
      }
    stopwatch.stop()
    log.debug("Query Execution time in ms: {}", stopwatch.time)
    return result
  }

  private fun buildQuery(query: String, whereClause: String, sortColumn: String, sortingDirection: String, pageSize: Long, selectedPage: Long) =
    """SELECT *
        FROM ($query) Q
        $whereClause
        ORDER BY $sortColumn $sortingDirection 
                      limit $pageSize OFFSET ($selectedPage - 1) * $pageSize;"""

  fun count(
    filters: List<Filter>,
    query: String,
    userCaseloads: List<String>,
    caseloadFields: List<String>,
    reportId: String,
  ): Long {
    val (preparedStatementNamedParams, whereClause) = buildWhereClause(filters, userCaseloads, caseloadFields, reportId)
    return jdbcTemplate.queryForList(
      "SELECT count(*) as total FROM ($query) Q $whereClause",
      preparedStatementNamedParams,
    ).first()?.get("total") as Long
  }

  private fun transformTimestampToLocalDateTime(it: MutableMap<String, Any>) = it.entries.associate { (k, v) ->
    if (v is Timestamp) {
      k to v.toLocalDateTime()
    } else {
      k to v
    }
  }

  private fun buildWhereClause(
    filters: List<Filter>,
    userCaseloads: List<String>,
    caseloadFields: List<String>,
    reportId: String,
  ): Pair<MapSqlParameterSource, String> {
    val preparedStatementNamedParams = MapSqlParameterSource()
    filters.forEach { preparedStatementNamedParams.addValue(it.getKey(), it.value.lowercase()) }
    val allFilters = filters.joinToString(" AND ", transform = this::buildCondition)
    val caseloadsStringArray = "(${userCaseloads.joinToString { "\'$it\'" }})"
    val caseloadsWhereClause = when (reportId) {
      EXTERNAL_MOVEMENTS_PRODUCT_ID -> "(origin_code IN $caseloadsStringArray AND lower(direction)='out') OR (destination_code IN $caseloadsStringArray AND lower(direction)='in')"
      else -> buildCaseloadsWhereClause(userCaseloads, caseloadFields)
    }
    val whereClause = if (allFilters.isEmpty()) { "WHERE $caseloadsWhereClause" } else allFilters.let { "WHERE $it AND ($caseloadsWhereClause)" }
    return Pair(preparedStatementNamedParams, whereClause)
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
    }
  }

  private fun buildCaseloadsWhereClause(caseloads: List<String>, caseloadFields: List<String>): String {
    if (caseloadFields.isEmpty()) {
      return "TRUE"
    }
    val caseloadsStringArray = "(${caseloads.joinToString(",") { "\'$it\'" }})"
    return caseloadFields.joinToString(" OR ") { "$it IN $caseloadsStringArray" }
  }

  data class Filter(
    val field: String,
    val value: String,
    val type: FilterType = FilterType.STANDARD,
  ) {
    fun getKey(): String = "${this.field}${this.type.suffix}".lowercase()
  }

  enum class FilterType(val suffix: String) {
    STANDARD(""),
    RANGE_START(".start"),
    RANGE_END(".end"),
    DATE_RANGE_START(".start"),
    DATE_RANGE_END(".end"),
  }
}
