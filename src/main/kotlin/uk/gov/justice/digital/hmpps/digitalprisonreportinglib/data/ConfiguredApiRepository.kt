package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import jakarta.validation.ValidationException
import org.apache.commons.lang3.time.StopWatch
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.RANGE_FILTER_END_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.RANGE_FILTER_START_SUFFIX
import java.sql.Timestamp

@Service
@Suppress("UNCHECKED_CAST")
class ConfiguredApiRepository {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Autowired
  lateinit var jdbcTemplate: NamedParameterJdbcTemplate
  fun executeQuery(
    query: String,
    rangeFilters: Map<String, String>,
    filtersExcludingRange: Map<String, String>,
    selectedPage: Long,
    pageSize: Long,
    sortColumn: String,
    sortedAsc: Boolean,
    caseloads: List<String>,
  ): List<Map<String, Any>> {
    if (caseloads.isEmpty()) {
      log.warn("Zero records returned as the user has no active caseloads.")
      return emptyList()
    }
    val (preparedStatementNamedParams, whereClause) = buildWhereClause(filtersExcludingRange, rangeFilters, caseloads)
    val sortingDirection = if (sortedAsc) "asc" else "desc"
    val stopwatch = StopWatch.createStarted()
    val result = jdbcTemplate.queryForList(
      buildQuery(query, whereClause, sortColumn, sortingDirection, pageSize, selectedPage),
      preparedStatementNamedParams,
    )
      .map {
        transformTimestampToString(it)
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
    rangeFilters: Map<String, String>,
    filtersExcludingRange: Map<String, String>,
    query: String,
    caseloads: List<String>,
  ): Long {
    val (preparedStatementNamedParams, whereClause) = buildWhereClause(filtersExcludingRange, rangeFilters, caseloads)
    return jdbcTemplate.queryForList(
      "SELECT count(*) as total FROM ($query) $whereClause",
      preparedStatementNamedParams,
    ).first()?.get("total") as Long
  }

  private fun transformTimestampToString(it: MutableMap<String, Any>) = it.entries.associate { (k, v) ->
    if (v is Timestamp) {
      k to v.toLocalDateTime().toLocalDate().toString()
    } else {
      k to v
    }
  }

  private fun buildWhereClause(filtersExcludingRange: Map<String, String>, rangeFilters: Map<String, String>, caseloads: List<String>): Pair<MapSqlParameterSource, String> {
    val preparedStatementNamedParams = MapSqlParameterSource()
    filtersExcludingRange.forEach { preparedStatementNamedParams.addValue(it.key, it.value.lowercase()) }
    rangeFilters.forEach { preparedStatementNamedParams.addValue(it.key, it.value) }
    val whereNoRange = filtersExcludingRange.keys.joinToString(" AND ") { k -> "lower($k) = :$k" }.ifEmpty { null }
    val whereRange = buildWhereRangeCondition(rangeFilters)
    //    val whereClause = whereNoRange?.let { "WHERE ${whereRange?.let { "$whereNoRange AND $whereRange"} ?: whereNoRange}" } ?: whereRange?.let { "WHERE $it" } ?: ""
    val allFilters = whereNoRange?.plus(whereRange?.let { " AND $it" } ?: "") ?: whereRange
    val caseloadsStringArray = "(${caseloads.map { "\'$it\'" }.joinToString()})"
    val caseloadsWhereClause = "origin IN $caseloadsStringArray OR  destination IN $caseloadsStringArray"
    val whereClause = allFilters?.let { "WHERE $it AND ($caseloadsWhereClause)" } ?: "WHERE $caseloadsWhereClause"
    return Pair(preparedStatementNamedParams, whereClause)
  }

  private fun buildWhereRangeCondition(rangeFilters: Map<String, String>) =
    rangeFilters.keys.joinToString(" AND ") { k ->
      if (k.endsWith("$RANGE_FILTER_START_SUFFIX")) {
        "${k.removeSuffix("$RANGE_FILTER_START_SUFFIX")} >= :$k"
      } else if (k.endsWith("$RANGE_FILTER_END_SUFFIX")) {
        "${k.removeSuffix("$RANGE_FILTER_END_SUFFIX")} <= :$k"
      } else {
        throw ValidationException("Range filter does not have a .start or .end suffix: $k")
      }
    }
      .ifEmpty { null }
}
