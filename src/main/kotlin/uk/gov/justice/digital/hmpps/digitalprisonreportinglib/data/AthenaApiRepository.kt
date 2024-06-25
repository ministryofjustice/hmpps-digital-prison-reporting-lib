package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.AthenaError
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.QueryExecutionContext
import software.amazon.awssdk.services.athena.model.QueryExecutionStatus
import software.amazon.awssdk.services.athena.model.ResultConfiguration
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionStatus
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.TableIdGenerator

@Service
class AthenaApiRepository(
  val athenaClient: AthenaClient,
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
    dynamicFilterFieldId: Set<String>?,
    database: String?,
    catalog: String?,
  ): StatementExecutionResponse {
    val tableId = tableIdGenerator.generateNewExternalTableId()
    val queryExecutionContext = QueryExecutionContext.builder()
      .database(database)
      .catalog(catalog)
      .build()

    val finalQuery = """
          CREATE TABLE AwsDataCatalog.reports.$tableId 
          WITH (
            format = 'PARQUET'
          ) 
          AS (
          SELECT * FROM TABLE(system.query(query =>
           '${
      buildFinalQuery(
        buildReportQuery(query),
        buildPolicyQuery(policyEngineResult),
        // The filters part will be replaced with the variables CTE
        "$FILTER_ AS (SELECT * FROM $POLICY_ WHERE $TRUE_WHERE_CLAUSE)",
        buildFinalStageQuery(dynamicFilterFieldId, sortColumn, sortedAsc),
      ).replace("'", "''")
    }'
           )) 
          );
    """.trimIndent()
    val resultConfiguration = ResultConfiguration.builder()
      .outputLocation("s3://$s3location/$tableId/")
      .build()
    val startQueryExecutionRequest = StartQueryExecutionRequest.builder()
      .queryString(finalQuery)
      .queryExecutionContext(queryExecutionContext)
      .resultConfiguration(resultConfiguration)
      .build()
    val queryExecutionId = athenaClient
      .startQueryExecution(startQueryExecutionRequest).queryExecutionId()
    return StatementExecutionResponse(tableId, queryExecutionId)
  }

  override fun getStatementStatus(statementId: String): StatementExecutionStatus {
    val getQueryExecutionRequest = GetQueryExecutionRequest.builder()
      .queryExecutionId(statementId)
      .build()

    val getQueryExecutionResponse = athenaClient.getQueryExecution(getQueryExecutionRequest)
    val status = getQueryExecutionResponse.queryExecution().status()
    val stateChangeReason = status.stateChangeReason()
    val error: AthenaError? = status.athenaError()
    return StatementExecutionStatus(
      status = mapAthenaStateToRedshiftState(status.state().toString()),
      duration = calculateDuration(status),
      queryString = getQueryExecutionResponse.queryExecution().query(),
      resultRows = 0,
      resultSize = 0,
      error = error?.errorMessage(),
      errorCategory = error?.errorCategory(),
      stateChangeReason = stateChangeReason,
    )
  }

  private fun mapAthenaStateToRedshiftState(queryState: String): String {
    val athenaToRedshiftStateMappings = mapOf(
      "QUEUED" to "SUBMITTED",
      "RUNNING" to "STARTED",
      "SUCCEEDED" to "FINISHED",
      "CANCELLED" to "ABORTED",
    )
    return athenaToRedshiftStateMappings.getOrDefault(queryState, queryState)
  }

  private fun calculateDuration(status: QueryExecutionStatus): Long {
    return status.completionDateTime()?.let { completion ->
      status.submissionDateTime()?.let { submission ->
        completion.minusMillis(submission.toEpochMilli()).toEpochMilli() * 1000000
      } ?: 0
    } ?: 0
  }
}
