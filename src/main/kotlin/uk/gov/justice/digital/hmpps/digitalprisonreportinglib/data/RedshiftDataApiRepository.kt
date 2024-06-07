package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient
import software.amazon.awssdk.services.redshiftdata.model.DescribeStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementResponse
import software.amazon.awssdk.services.redshiftdata.model.SqlParameter
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionStatus
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.TableIdGenerator

@Service
class RedshiftDataApiRepository(
  val redshiftDataClient: RedshiftDataClient,
  val executeStatementRequestBuilder: ExecuteStatementRequest.Builder,
  val tableIdGenerator: TableIdGenerator,
  @Value("\${dpr.lib.redshiftdataapi.s3location:#{'dpr-working-development/reports'}}")
  private val s3location: String = "dpr-working-development/reports",
) : AthenaAndRedshiftCommonRepository() {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
  override fun executeQueryAsync(
    query: String,
    filters: List<ConfiguredApiRepository.Filter>,
    sortColumn: String?,
    sortedAsc: Boolean,
    policyEngineResult: String,
    dynamicFilterFieldId: String?,
    database: String?,
    catalog: String?,
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

  override fun getStatementStatus(statementId: String): StatementExecutionStatus {
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

  private fun buildQueryParams(filters: List<ConfiguredApiRepository.Filter>): List<SqlParameter> {
    val sqlParams: MutableList<SqlParameter> = mutableListOf()
    filters.filterNot { it.type == FilterType.BOOLEAN }.forEach { sqlParams.add(SqlParameter.builder().name(maybeTransform(it.getKey(), queryParamKeyTransformer)).value(it.value.lowercase()).build()) }
    filters.filter { it.type == FilterType.BOOLEAN }.forEach { sqlParams.add(SqlParameter.builder().name(maybeTransform(it.getKey(), queryParamKeyTransformer)).value(it.value).build()) }
    log.debug("SQL parameters: {}", sqlParams)
    return sqlParams
  }

  private val queryParamKeyTransformer: (s: String) -> String = { s -> s.replace(".", "_") }
}
