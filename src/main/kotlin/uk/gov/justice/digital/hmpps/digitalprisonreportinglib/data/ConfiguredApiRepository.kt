package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.apache.commons.lang3.time.StopWatch
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementResponse
import software.amazon.awssdk.services.redshiftdata.model.SqlParameter
import java.sql.Timestamp
import javax.sql.DataSource


@Service
class ConfiguredApiRepository(
  @Value("\${redshiftdataapi.database:#{null}}") val redshiftDataApiDb: String? = null,
  @Value("\${redshiftdataapi.clusterid:#{null}}") val redshiftDataApiClusterId: String? = null,
  @Value("\${redshiftdataapi.secretarn:#{null}}") val redshiftDataApiSecretArn: String? = null,

) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    const val EXTERNAL_MOVEMENTS_PRODUCT_ID = "external-movements"
    private const val DATASET_ = """dataset_"""
    private const val POLICY_ = """policy_"""
    private const val FILTER_ = """filter_"""
  }

  @Autowired
  lateinit var context: ApplicationContext

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
    dataSourceName: String,
  ): List<Map<String, Any?>> {
    val stopwatch = StopWatch.createStarted()
    val jdbcTemplate = populateJdbcTemplate(dataSourceName)
    // The result of the query can contain null values.
    // This is coming from Java and if the returned type is not specified in Kotlin it will assume it is List<Map<String, Any>>
    // while in reality it is List<Map<String, Any?>>.
    val result: List<Map<String, Any?>> = jdbcTemplate.queryForList(
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

  fun executeQueryAsync(query: String,
                        filters: List<Filter>,
                        sortColumn: String?,
                        sortedAsc: Boolean,
                        reportId: String,
                        policyEngineResult: String,
                        dynamicFilterFieldId: String? = null,
                        dataSourceName: String,): String {
    val redshiftDataClient = RedshiftDataClient.builder()
      .region(Region.EU_WEST_2)
      .build()
    val sqlStatement = " SELECT * FROM datamart.domain.movement_movement"
//    val yearParam: SqlParameter = SqlParameter.builder()
//      .name("year")
//      .value(java.lang.String.valueOf(year))
//      .build()

    val statementRequest: ExecuteStatementRequest = ExecuteStatementRequest.builder()
      .clusterIdentifier(redshiftDataApiClusterId)
      .database(redshiftDataApiDb)
//      .dbUser(dbUser)
      .secretArn(redshiftDataApiSecretArn)
//      .parameters(yearParam)
      .sql(sqlStatement)
      .build()

    val response: ExecuteStatementResponse = redshiftDataClient.executeStatement(statementRequest)
    log.info("Execution ID: {}", response.id())
    return response.id()
  }

  private fun populateJdbcTemplate(dataSourceName: String): NamedParameterJdbcTemplate {
    val dataSource = if (context.containsBean(dataSourceName)) {
      context.getBean(dataSourceName, DataSource::class) as DataSource
    } else {
      log.warn("No DataSource Bean found with name: {}", dataSourceName)
      context.getBean(DataSource::class.java) as DataSource
    }

    return NamedParameterJdbcTemplate(dataSource)
  }

  private fun buildReportQuery(query: String) = """WITH $DATASET_ AS ($query)"""
  private fun buildPolicyQuery(policyEngineResult: String) = """$POLICY_ AS (SELECT * FROM $DATASET_ WHERE $policyEngineResult)"""
  private fun buildFiltersQuery(filters: List<Filter>) =
    """$FILTER_ AS (SELECT * FROM $POLICY_ WHERE ${buildFiltersWhereClause(filters)})"""
  private fun buildPaginationQuery(
    dynamicFilterFieldId: String?,
    sortColumn: String?,
    sortedAsc: Boolean,
    pageSize: Long,
    selectedPage: Long,
  ) = """SELECT ${constructProjectedColumns(dynamicFilterFieldId)}
        FROM $FILTER_ ${buildOrderByClause(sortColumn, sortedAsc)} 
        limit $pageSize OFFSET ($selectedPage - 1) * $pageSize;"""

  private fun buildOrderByClause(sortColumn: String?, sortedAsc: Boolean) =
    sortColumn?.let { """ORDER BY $sortColumn ${calculateSortingDirection(sortedAsc)}""" } ?: ""

  private fun buildFinalQuery(
    reportQuery: String,
    policiesQuery: String,
    filtersQuery: String,
    selectFromFinalStageQuery: String,
  ): String {
    val query = listOf(reportQuery, policiesQuery, filtersQuery).joinToString(",") + "\n$selectFromFinalStageQuery"
    log.debug("Database query: $query")
    return query
  }

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
    dataSourceName: String,
  ): Long {
    val jdbcTemplate = populateJdbcTemplate(dataSourceName)
    return jdbcTemplate.queryForList(
      buildFinalQuery(
        buildReportQuery(query),
        buildPolicyQuery(policyEngineResult),
        buildFiltersQuery(filters),
        "SELECT COUNT(1) as total FROM $FILTER_",
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
    val filterClause = filters.joinToString(" AND ", transform = this::buildCondition).ifEmpty { "TRUE" }
    log.debug("Filter clause: {}", filterClause)
    return filterClause
  }

  fun buildPreparedStatementNamedParams(filters: List<Filter>): MapSqlParameterSource {
    val preparedStatementNamedParams = MapSqlParameterSource()
    filters.filterNot { it.type == FilterType.BOOLEAN }.forEach { preparedStatementNamedParams.addValue(it.getKey(), it.value.lowercase()) }
    filters.filter { it.type == FilterType.BOOLEAN }.forEach { preparedStatementNamedParams.addValue(it.getKey(), it.value.toBoolean()) }
    log.debug("Prepared statement named parameters: {}", preparedStatementNamedParams)
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
      FilterType.DYNAMIC -> "${filter.field} ILIKE '${filter.value}%'"
      FilterType.BOOLEAN -> "${filter.field} = :$key"
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
    BOOLEAN,
  }
}
