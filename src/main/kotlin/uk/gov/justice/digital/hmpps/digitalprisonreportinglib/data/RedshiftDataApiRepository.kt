package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.apache.commons.lang3.time.StopWatch
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient
import software.amazon.awssdk.services.redshiftdata.model.ColumnMetadata
import software.amazon.awssdk.services.redshiftdata.model.DescribeStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementResponse
import software.amazon.awssdk.services.redshiftdata.model.Field
import software.amazon.awssdk.services.redshiftdata.model.GetStatementResultResponse
import software.amazon.awssdk.services.redshiftdata.model.SqlParameter
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionStatus
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.TableIdGenerator
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class RedshiftDataApiRepository(
  val redshiftDataClient: RedshiftDataClient,
  val executeStatementRequestBuilder: ExecuteStatementRequest.Builder,
  val tableIdGenerator: TableIdGenerator,
  @Value("\${dpr.lib.redshiftdataapi.s3location:#{'dpr-working-development/reports'}}")
  private val s3location: String = "dpr-working-development/reports",
) : RepositoryHelper() {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
  fun executeQueryAsync(
    query: String,
    filters: List<ConfiguredApiRepository.Filter>,
    sortColumn: String?,
    sortedAsc: Boolean,
    reportId: String,
    policyEngineResult: String,
    dynamicFilterFieldId: String? = null,
    dataSourceName: String,
  ): StatementExecutionResponse {
    val tableId = tableIdGenerator.generateNewExternalTableId()
    val generateSql = """
          CREATE EXTERNAL TABLE reports.$tableId 
          STORED AS parquet 
          LOCATION 's3://$s3location/$tableId/' 
          AS ( 
          ${
      buildFinalQuery(
        buildReportQuery(query),
        buildPolicyQuery(policyEngineResult),
        buildFiltersQuery(filters, queryParamKeyTransformer),
        buildFinalStageQuery(dynamicFilterFieldId, sortColumn, sortedAsc),
      )
    }
          );
    """.trimIndent()
    val requestBuilder = executeStatementRequestBuilder
      .sql(
        generateSql,
      )
    if (filters.isNotEmpty()) {
      requestBuilder
        .parameters(buildQueryParams(filters))
    }
    val statementRequest: ExecuteStatementRequest = requestBuilder.build()

    val response: ExecuteStatementResponse = redshiftDataClient.executeStatement(statementRequest)
    log.debug("Execution ID: {}", response.id())
    log.debug("External table ID: {}", tableId)
    return StatementExecutionResponse(tableId, response.id())
  }

  fun getStatementStatus(statementId: String): StatementExecutionStatus {
    val statementRequest = DescribeStatementRequest.builder()
      .id(statementId)
      .build()
    val describeStatementResponse = redshiftDataClient.describeStatement(statementRequest)
    return StatementExecutionStatus(
      status = describeStatementResponse.statusAsString(),
      duration = describeStatementResponse.duration(),
      queryString = describeStatementResponse.queryString(),
      resultRows = describeStatementResponse.resultRows(),
      resultSize = describeStatementResponse.resultSize(),
      error = describeStatementResponse.error(),
    )
  }

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

  private fun extractRecords(resultStatementResponse: GetStatementResultResponse) =
    resultStatementResponse.records().map { record ->
      record.mapIndexed { index, field -> extractFieldValue(field, resultStatementResponse.columnMetadata()[index]) }
        .toMap()
    }

  private fun extractFieldValue(field: Field, columnMetadata: ColumnMetadata): Pair<String, Any?> {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val value = when (columnMetadata.typeName()) {
      "varchar" -> field.stringValue()
      "int8" -> field.longValue()
      "timestamp" -> LocalDateTime.parse(field.stringValue(), formatter)
      // This will need to be extended to support more date types when required in the future.
      else -> field.stringValue()
    }
    return columnMetadata.name() to value
  }

  private fun buildQueryParams(filters: List<ConfiguredApiRepository.Filter>): List<SqlParameter> {
    val sqlParams: MutableList<SqlParameter> = mutableListOf()
    filters.filterNot { it.type == FilterType.BOOLEAN }.forEach { sqlParams.add(SqlParameter.builder().name(maybeTransform(it.getKey(), queryParamKeyTransformer)).value(it.value.lowercase()).build()) }
    filters.filter { it.type == FilterType.BOOLEAN }.forEach { sqlParams.add(SqlParameter.builder().name(maybeTransform(it.getKey(), queryParamKeyTransformer)).value(it.value).build()) }
    log.debug("SQL parameters: {}", sqlParams)
    return sqlParams
  }

  val queryParamKeyTransformer: (s: String) -> String = { s -> s.replace(".", "_") }
}
