package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.DataApiSyncController.FiltersPrefix.RANGE_FILTER_END_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.DataApiSyncController.FiltersPrefix.RANGE_FILTER_START_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportFilter
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy.PolicyResult
import java.sql.Timestamp
import javax.sql.DataSource

abstract class RepositoryHelper {
  companion object {
    @JvmStatic
    protected val log: Logger = LoggerFactory.getLogger(this::class.java)
    const val EXTERNAL_MOVEMENTS_PRODUCT_ID = "external-movements"
    const val TRUE_WHERE_CLAUSE = "1=1"
    const val FALSE_WHERE_CLAUSE = "0=1"

    const val DATASET_ = """dataset_"""
    const val REPORT_ = """report_"""
    const val POLICY_ = """policy_"""
    const val FILTER_ = """filter_"""
    const val PROMPT = """prompt_"""
    const val CONTEXT = """context_"""

    const val DEFAULT_REPORT_CTE = "report_ AS (SELECT * FROM dataset_)"
  }

  @Autowired
  lateinit var context: ApplicationContext

  protected fun populateNamedParameterJdbcTemplate(dataSourceName: String? = null): NamedParameterJdbcTemplate {
    val dataSource = findDataSource(dataSourceName)
    return NamedParameterJdbcTemplate(dataSource)
  }

  protected fun populateJdbcTemplate(dataSourceName: String? = null): JdbcTemplate {
    val dataSource = findDataSource(dataSourceName)
    return JdbcTemplate(dataSource)
  }

  private fun findDataSource(dataSourceName: String?): DataSource {
    val dataSource = if (dataSourceName == null) {
      context.getBean(DataSource::class.java) as DataSource
    } else if (context.containsBean(dataSourceName)) {
      context.getBean(dataSourceName, DataSource::class) as DataSource
    } else {
      log.warn("No DataSource Bean found with name: {}", dataSourceName)
      context.getBean(DataSource::class.java) as DataSource
    }
    return dataSource
  }

  protected fun transformTimestampToLocalDateTime(it: MutableMap<String, Any>) = it.entries.associate { (k, v) ->
    if (v is Timestamp) {
      k to v.toLocalDateTime()
    } else {
      k to v
    }
  }

  protected fun buildFinalQuery(
    datasetQuery: String,
    reportQuery: String,
    policiesQuery: String,
    filtersQuery: String,
    selectFromFinalStageQuery: String,
  ): String {
    val query = listOf(datasetQuery, reportQuery, policiesQuery, filtersQuery).joinToString(",") + "\n$selectFromFinalStageQuery"
    log.debug("Database query: $query")
    return query
  }

  protected open fun buildDatasetQuery(query: String) = """WITH $DATASET_ AS ($query)"""

  protected fun buildReportQuery(filter: ReportFilter?): String {
    return filter?.query ?: DEFAULT_REPORT_CTE
  }

  protected fun determinePreviousCteName(reportFilter: ReportFilter? = null) =
    reportFilter?.name ?: REPORT_

  protected fun buildPolicyQuery(policyEngineResult: String, previousCteName: String? = DATASET_) =
    """$POLICY_ AS (SELECT * FROM $previousCteName WHERE ${convertPolicyResultToSql(policyEngineResult)})"""
  protected fun buildFiltersQuery(filters: List<ConfiguredApiRepository.Filter>) =
    """$FILTER_ AS (SELECT * FROM $POLICY_ WHERE ${buildFiltersWhereClause(filters)})"""

  protected fun buildFinalStageQuery(
    dynamicFilterFieldId: Set<String>? = null,
    sortColumn: String? = null,
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

  private fun convertPolicyResultToSql(policyEngineResult: String): String {
    return policyEngineResult
      .replace(PolicyResult.POLICY_PERMIT, TRUE_WHERE_CLAUSE)
      .replace(PolicyResult.POLICY_DENY, FALSE_WHERE_CLAUSE)
  }

  protected fun buildFiltersWhereClause(
    filters: List<ConfiguredApiRepository.Filter>,
  ): String {
    val filterClause = filters.joinToString(" AND ") { this.buildCondition(it) }.ifEmpty { TRUE_WHERE_CLAUSE }
    log.debug("Filter clause: {}", filterClause)
    return filterClause
  }

  private fun constructProjectedColumns(dynamicFilterFieldId: Set<String>?) =
    dynamicFilterFieldId?.let { "DISTINCT ${dynamicFilterFieldId.joinToString(", ")}" } ?: "*"

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
