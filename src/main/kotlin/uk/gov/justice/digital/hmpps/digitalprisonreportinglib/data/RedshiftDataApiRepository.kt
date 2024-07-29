package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.apache.commons.lang3.time.StopWatch
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient
import software.amazon.awssdk.services.redshiftdata.model.CancelStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.DescribeStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Datasource
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementCancellationResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionStatus
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.TableIdGenerator

@Service
class RedshiftDataApiRepository(
  val redshiftDataClient: RedshiftDataClient,
  private val tableIdGenerator: TableIdGenerator,
  private val datasetHelper: DatasetHelper,
  @Value("\${dpr.lib.redshiftdataapi.database:db}") private val redshiftDataApiDb: String,
  @Value("\${dpr.lib.redshiftdataapi.clusterid:clusterId}") private val redshiftDataApiClusterId: String,
  @Value("\${dpr.lib.redshiftdataapi.secretarn:arn}") private val redshiftDataApiSecretArn: String,
  @Value("\${dpr.lib.redshiftdataapi.s3location:#{'dpr-working-development/reports'}}")
  private val s3location: String = "dpr-working-development/reports",
) : AthenaAndRedshiftCommonRepository(tableIdGenerator) {
  override fun executeQueryAsync(
    productDefinition: SingleReportProductDefinition,
    filters: List<ConfiguredApiRepository.Filter>,
    sortColumn: String?,
    sortedAsc: Boolean,
    policyEngineResult: String,
    dynamicFilterFieldId: Set<String>?,
    prompts: Map<String, String>?,
    userToken: DprAuthAwareAuthenticationToken?,
  ): StatementExecutionResponse {
    val tableId = tableIdGenerator.generateNewExternalTableId()
    val generateSql = """
          CREATE EXTERNAL TABLE reports.$tableId 
          STORED AS parquet 
          LOCATION 's3://$s3location/$tableId/' 
          AS ( 
          ${
      buildFinalQuery(
        buildReportQuery(productDefinition.reportDataset.query),
        buildPolicyQuery(policyEngineResult),
        buildFiltersQuery(filters),
        buildFinalStageQuery(dynamicFilterFieldId, sortColumn, sortedAsc),
      )
    }
          );
          ${buildSummaryQueries(productDefinition, tableId)}
    """.trimIndent()

    return executeQueryAsync(productDefinition.datasource, tableId, generateSql)
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
    }
  }

  fun buildSummaryQueries(productDefinition: SingleReportProductDefinition, tableId: String): String {
    return productDefinition.report.summary?.joinToString(" ") {
      val summaryTableId = tableIdGenerator.getTableSummaryId(tableId, it.id)
      val query = datasetHelper.findDataset(productDefinition.allDatasets, it.dataset).query
      val substitutedQuery = query.replace(TABLE_TOKEN_NAME, "reports.$tableId")

      buildSummaryQuery(
        substitutedQuery,
        summaryTableId,
      )
    } ?: ""
  }

  override fun buildSummaryQuery(query: String, summaryTableId: String): String {
    return """
          CREATE EXTERNAL TABLE reports.$summaryTableId 
          STORED AS parquet 
          LOCATION 's3://$s3location/$summaryTableId/' 
          AS ($query);
    """
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
}
