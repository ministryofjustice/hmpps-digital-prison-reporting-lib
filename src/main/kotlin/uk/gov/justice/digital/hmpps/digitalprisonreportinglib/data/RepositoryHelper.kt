package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.RANGE_FILTER_END_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.RANGE_FILTER_START_SUFFIX
import java.sql.Timestamp
import javax.sql.DataSource

abstract class RepositoryHelper {
  companion object {
    @JvmStatic
    protected val log: Logger = LoggerFactory.getLogger(this::class.java)
    const val EXTERNAL_MOVEMENTS_PRODUCT_ID = "external-movements"

    const val DATASET_ = """dataset_"""
    const val POLICY_ = """policy_"""
    const val FILTER_ = """filter_"""
  }

  @Autowired
  lateinit var context: ApplicationContext

  protected fun populateJdbcTemplate(dataSourceName: String? = null): NamedParameterJdbcTemplate {
    val dataSource = if (dataSourceName == null) {
      context.getBean(DataSource::class.java) as DataSource
    } else if (context.containsBean(dataSourceName)) {
      context.getBean(dataSourceName, DataSource::class) as DataSource
    } else {
      log.warn("No DataSource Bean found with name: {}", dataSourceName)
      context.getBean(DataSource::class.java) as DataSource
    }

    return NamedParameterJdbcTemplate(dataSource)
  }

  protected fun transformTimestampToLocalDateTime(it: MutableMap<String, Any>) = it.entries.associate { (k, v) ->
    if (v is Timestamp) {
      k to v.toLocalDateTime()
    } else {
      k to v
    }
  }

  protected fun buildFinalQuery(
    reportQuery: String,
    policiesQuery: String,
    filtersQuery: String,
    selectFromFinalStageQuery: String,
  ): String {
    val query = listOf(reportQuery, policiesQuery, filtersQuery).joinToString(",") + "\n$selectFromFinalStageQuery"
    log.debug("Database query: $query")
    return query
  }

  protected fun buildReportQuery(query: String) = """WITH $DATASET_ AS ($query)"""
  protected fun buildPolicyQuery(policyEngineResult: String) = """$POLICY_ AS (SELECT * FROM $DATASET_ WHERE $policyEngineResult)"""
  protected fun buildFiltersQuery(filters: List<ConfiguredApiRepository.Filter>) =
    """$FILTER_ AS (SELECT * FROM $POLICY_ WHERE ${buildFiltersWhereClause(filters)})"""

  private fun buildFiltersWhereClause(
    filters: List<ConfiguredApiRepository.Filter>,
  ): String {
    val filterClause = filters.joinToString(" AND ") { this.buildCondition(it) }.ifEmpty { "TRUE" }
    log.debug("Filter clause: {}", filterClause)
    return filterClause
  }

  protected fun buildFinalStageQuery(
    dynamicFilterFieldId: String?,
    sortColumn: String?,
    sortedAsc: Boolean,
  ) = """SELECT ${constructProjectedColumns(dynamicFilterFieldId)}
          FROM $FILTER_ ${buildOrderByClause(sortColumn, sortedAsc)}"""

  protected open fun buildCondition(filter: ConfiguredApiRepository.Filter): String {
    val lowerCaseField = "lower(${filter.field})"
    val key = filter.getKey()

    return when (filter.type) {
      FilterType.STANDARD -> "$lowerCaseField = :$key"
      FilterType.RANGE_START -> "$lowerCaseField >= :$key"
      FilterType.DATE_RANGE_START -> "${filter.field} >= CAST(:$key AS timestamp)"
      FilterType.RANGE_END -> "$lowerCaseField <= :$key"
      FilterType.DATE_RANGE_END -> "${filter.field} < (CAST(:$key AS timestamp) + INTERVAL '1' day)"
      FilterType.DYNAMIC -> "${filter.field} ILIKE '${filter.value}%'"
      FilterType.BOOLEAN -> "${filter.field} = :$key"
    }
  }

  protected fun maybeTransform(key: String, keyTransformer: ((s: String) -> String)?) =
    keyTransformer?.let { it(key) } ?: key

  private fun constructProjectedColumns(dynamicFilterFieldId: String?) =
    dynamicFilterFieldId?.let { "DISTINCT $dynamicFilterFieldId" } ?: "*"

  private fun buildOrderByClause(sortColumn: String?, sortedAsc: Boolean) =
    sortColumn?.let { """ORDER BY $sortColumn ${calculateSortingDirection(sortedAsc)}""" } ?: ""

  private fun calculateSortingDirection(sortedAsc: Boolean): String {
    return if (sortedAsc) "asc" else "desc"
  }

  enum class FilterType(val suffix: String = "") {
    STANDARD,
    RANGE_START(RANGE_FILTER_START_SUFFIX),
    RANGE_END(RANGE_FILTER_END_SUFFIX),
    DATE_RANGE_START(RANGE_FILTER_START_SUFFIX),
    DATE_RANGE_END(RANGE_FILTER_END_SUFFIX),
    DYNAMIC,
    BOOLEAN,
  }
}
