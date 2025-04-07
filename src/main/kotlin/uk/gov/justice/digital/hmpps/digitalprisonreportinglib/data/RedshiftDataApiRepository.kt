package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.apache.commons.lang3.time.StopWatch
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient
import software.amazon.awssdk.services.redshiftdata.model.CancelStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.DescribeStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.DatasetQuery
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Datasource
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportFilter
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportSummary
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleDashboardProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementCancellationResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionStatus
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.TableIdGenerator
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Prompt

@Service
@ConditionalOnBean(AthenaClient::class)
class RedshiftDataApiRepository(
  val redshiftDataClient: RedshiftDataClient,
  private val tableIdGenerator: TableIdGenerator,
  private val identifiedHelper: IdentifiedHelper,
  private val redShiftSummaryTableHelper: RedShiftSummaryTableHelper,
  @Value("\${dpr.lib.redshiftdataapi.database:db}") private val redshiftDataApiDb: String,
  @Value("\${dpr.lib.redshiftdataapi.clusterid:clusterId}") private val redshiftDataApiClusterId: String,
  @Value("\${dpr.lib.redshiftdataapi.secretarn:arn}") private val redshiftDataApiSecretArn: String,
  @Value("\${dpr.lib.redshiftdataapi.s3location:#{'dpr-working-development/reports'}}")
  private val s3location: String = "dpr-working-development/reports",
) : AthenaAndRedshiftCommonRepository() {
  override fun executeQueryAsync(
    filters: List<ConfiguredApiRepository.Filter>,
    sortColumn: String?,
    sortedAsc: Boolean,
    policyEngineResult: String,
    dynamicFilterFieldId: Set<String>?,
    prompts: List<Prompt>?,
    userToken: DprAuthAwareAuthenticationToken?,
    query: String,
    reportFilter: ReportFilter?,
    datasource: Datasource,
    reportSummaries: List<ReportSummary>?,
    allDatasets: List<Dataset>,
    productDefinitionId: String,
    productDefinitionName: String,
    reportOrDashboardId: String,
    reportOrDashboardName: String,
    preGeneratedDatasetTableId: String?,
    multiphaseQuery: List<DatasetQuery>?,
  ): StatementExecutionResponse {
    val tableId = tableIdGenerator.generateNewExternalTableId()
    val generateSql = """
          CREATE EXTERNAL TABLE reports.$tableId 
          STORED AS parquet 
          LOCATION 's3://$s3location/$tableId/' 
          AS ( 
          ${
      buildFinalQuery(
        datasetQuery = checkAndBuildDatasetQuery(query, preGeneratedDatasetTableId),
        reportQuery = buildReportQuery(reportFilter),
        policiesQuery = buildPolicyQuery(policyEngineResult, determinePreviousCteName(reportFilter)),
        filtersQuery = buildFiltersQuery(filters),
        selectFromFinalStageQuery = buildFinalStageQuery(dynamicFilterFieldId, sortColumn, sortedAsc),
      )
    }
          );
          ${buildSummaryQueries(tableId, reportSummaries, allDatasets)}
    """.trimIndent()

    return executeQueryAsync(datasource, tableId, generateSql)
  }

  override fun executeQueryAsync(
    datasource: Datasource,
    tableId: String,
    query: String,
  ): StatementExecutionResponse {
    val statementRequest = ExecuteStatementRequest.builder()
      .clusterIdentifier(redshiftDataApiClusterId)
      .database(redshiftDataApiDb)
      .secretArn(redshiftDataApiSecretArn)
      .sql(query)
      .build()
    log.debug("Full async query: {}", query)
    val response: ExecuteStatementResponse = redshiftDataClient.executeStatement(statementRequest)
    log.debug("Execution ID: {}", response.id())
    log.debug("External table ID: {}", tableId)
    return StatementExecutionResponse(tableId, response.id())
  }

  override fun getStatementStatus(statementId: String): StatementExecutionStatus {
    val statementRequest = DescribeStatementRequest.builder()
      .id(statementId)
      .build()
    val describeStatementResponse = redshiftDataClient.describeStatement(statementRequest)
    return StatementExecutionStatus(
      status = describeStatementResponse.statusAsString(),
      duration = describeStatementResponse.duration(),
      resultRows = describeStatementResponse.resultRows(),
      resultSize = describeStatementResponse.resultSize(),
      error = describeStatementResponse.error(),
    )
  }

  override fun cancelStatementExecution(statementId: String): StatementCancellationResponse {
    val cancelStatementRequest = CancelStatementRequest.builder()
      .id(statementId)
      .build()
    val cancelStatementResponse = redshiftDataClient.cancelStatement(cancelStatementRequest)
    return StatementCancellationResponse(cancelStatementResponse.status())
  }

  override fun buildCondition(filter: ConfiguredApiRepository.Filter): String {
    val lowerCaseField = "lower(${filter.field})"
    return when (filter.type) {
      FilterType.STANDARD -> "$lowerCaseField = '${filter.value.lowercase()}'"
      FilterType.RANGE_START -> "$lowerCaseField >= ${filter.value.lowercase()}"
      FilterType.DATE_RANGE_START -> "${filter.field} >= CAST('${filter.value}' AS timestamp)"
      FilterType.RANGE_END -> "$lowerCaseField <= ${filter.value.lowercase()}"
      FilterType.DATE_RANGE_END -> "${filter.field} < (CAST('${filter.value}' AS timestamp) + INTERVAL '1' day)"
      FilterType.DYNAMIC -> "${filter.field} ILIKE '${filter.value}%'"
      FilterType.BOOLEAN -> "${filter.field} = ${filter.value.toBoolean()}"
      FilterType.MULTISELECT -> filter.value.split(",")
        .joinToString(separator = " OR ", prefix = "(", postfix = ")") { "${filter.field} = '$it'" }
    }
  }

  fun buildSummaryQueries(
    tableId: String,
    reportSummaries: List<ReportSummary>?,
    allDatasets: List<Dataset>,
  ): String = reportSummaries?.joinToString(" ") {
    val query = identifiedHelper.findOrFail(allDatasets, it.dataset).query

    redShiftSummaryTableHelper.buildSummaryQuery(
      query,
      tableId,
      it.id,
    )
  } ?: ""

  fun getFullExternalTableResult(
    tableId: String,
    jdbcTemplate: NamedParameterJdbcTemplate = populateNamedParameterJdbcTemplate(),
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

  fun count(
    tableId: String,
    filters: List<ConfiguredApiRepository.Filter> = emptyList(),
    jdbcTemplate: NamedParameterJdbcTemplate = populateNamedParameterJdbcTemplate(),
  ): Long {
    val whereClause = buildFiltersWhereClause(filters)
    return jdbcTemplate.queryForList(
      "SELECT COUNT(1) as total FROM reports.$tableId WHERE $whereClause;",
      MapSqlParameterSource(),
    ).first()?.get("total") as Long
  }

  fun executeQueryAsync(
    productDefinition: SingleDashboardProductDefinition,
    policyEngineResult: String,
    filters: List<ConfiguredApiRepository.Filter>,
  ): StatementExecutionResponse {
    val tableId = tableIdGenerator.generateNewExternalTableId()
    val generateSql = """
          CREATE EXTERNAL TABLE reports.$tableId 
          STORED AS parquet 
          LOCATION 's3://$s3location/$tableId/' 
          AS ( 
            ${buildFinalQuery(
      datasetQuery = buildDatasetQuery(productDefinition.dashboardDataset.query),
      reportQuery = DEFAULT_REPORT_CTE,
      policiesQuery = buildPolicyQuery(policyEngineResult, determinePreviousCteName()),
      filtersQuery = buildFiltersQuery(filters),
      selectFromFinalStageQuery = buildFinalStageQuery(sortedAsc = true),
    )}
          );
    """.trimIndent()

    return executeQueryAsync(productDefinition.datasource, tableId, generateSql)
  }

  private fun checkAndBuildDatasetQuery(query: String, generatedTableId: String?): String = generatedTableId?.let { tableId ->
    """WITH $DATASET_ AS (SELECT * FROM reports.$tableId)"""
  } ?: buildDatasetQuery(query)
}
