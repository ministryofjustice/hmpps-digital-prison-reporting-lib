package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient
import software.amazon.awssdk.services.redshiftdata.model.DescribeStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementResponse
import software.amazon.awssdk.services.redshiftdata.model.SqlParameter
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.StatementExecutionStatus

@Service
class RedshiftDataApiRepository(
  val redshiftDataClient: RedshiftDataClient,
  val executeStatementRequestBuilder: ExecuteStatementRequest.Builder,
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
  ): String {
    val requestBuilder = executeStatementRequestBuilder
      .sql(
        buildFinalQuery(
          buildReportQuery(query),
          buildPolicyQuery(policyEngineResult),
          buildFiltersQuery(filters),
          buildFinalStageQuery(dynamicFilterFieldId, sortColumn, sortedAsc),
        ),
      )
    if (filters.isNotEmpty()) {
      requestBuilder
        .parameters(buildQueryParams(filters))
    }
    val statementRequest: ExecuteStatementRequest = requestBuilder.build()

    val response: ExecuteStatementResponse = redshiftDataClient.executeStatement(statementRequest)
    log.info("Execution ID: {}", response.id())
    return response.id()
  }

  fun getStatementStatus(statementId: String): StatementExecutionStatus {
    val statementRequest = DescribeStatementRequest.builder()
      .id(statementId)
      .build()
    val describeStatementResponse = redshiftDataClient.describeStatement(statementRequest)
    return StatementExecutionStatus(
      status = describeStatementResponse.statusAsString(),
      error = describeStatementResponse.error(),
      duration = describeStatementResponse.duration(),
      queryString = describeStatementResponse.queryString(),
      resultRows = describeStatementResponse.resultRows(),
    )
  }

  private fun buildQueryParams(filters: List<ConfiguredApiRepository.Filter>): List<SqlParameter> {
    val sqlParams: MutableList<SqlParameter> = mutableListOf()
    filters.filterNot { it.type == FilterType.BOOLEAN }.forEach { sqlParams.add(SqlParameter.builder().name(it.getKey()).value(it.value.lowercase()).build()) }
    filters.filter { it.type == FilterType.BOOLEAN }.forEach { sqlParams.add(SqlParameter.builder().name(it.getKey()).value(it.value).build()) }
    log.debug("SQL parameters: {}", sqlParams)
    return sqlParams
  }
}
