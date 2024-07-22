package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.AthenaError
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.QueryExecutionContext
import software.amazon.awssdk.services.athena.model.QueryExecutionStatus
import software.amazon.awssdk.services.athena.model.ResultConfiguration
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.StopQueryExecutionRequest
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementCancellationResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionStatus
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.TableIdGenerator

@Service
class AthenaApiRepository(
  val athenaClient: AthenaClient,
  val tableIdGenerator: TableIdGenerator,
  datasetHelper: DatasetHelper,
  @Value("\${dpr.lib.redshiftdataapi.s3location:#{'dpr-working-development/reports'}}")
  private val s3location: String = "dpr-working-development/reports",
) : AthenaAndRedshiftCommonRepository(tableIdGenerator, datasetHelper) {

  override fun executeQueryAsync(
    productDefinition: SingleReportProductDefinition,
    filters: List<ConfiguredApiRepository.Filter>,
    sortColumn: String?,
    sortedAsc: Boolean,
    policyEngineResult: String,
    dynamicFilterFieldId: Set<String>?,
    prompts: Map<String, String>?,
  ): StatementExecutionResponse {
    val tableId = tableIdGenerator.generateNewExternalTableId()
    val queryExecutionContext = QueryExecutionContext.builder()
      .database(productDefinition.datasource.database)
      .catalog(productDefinition.datasource.catalog)
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
        buildPromptsQuery(prompts),
        buildReportQuery(productDefinition.reportDataset.query),
        buildPolicyQuery(policyEngineResult),
        // The filters part will be replaced with the variables CTE
        "$FILTER_ AS (SELECT * FROM $POLICY_ WHERE $TRUE_WHERE_CLAUSE)",
        buildFinalStageQuery(dynamicFilterFieldId, sortColumn, sortedAsc),
      ).replace("'", "''")
    }'
           )) 
          );
          ${buildSummaryQueries(productDefinition, tableId)}
    """.trimIndent()
    val resultConfiguration = ResultConfiguration.builder()
      .outputLocation("s3://$s3location/$tableId/")
      .build()
    val startQueryExecutionRequest = StartQueryExecutionRequest.builder()
      .queryString(finalQuery)
      .queryExecutionContext(queryExecutionContext)
      .resultConfiguration(resultConfiguration)
      .build()
    log.debug("Full async query: {}", finalQuery)
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
      resultRows = 0,
      resultSize = 0,
      error = error?.errorMessage(),
      errorCategory = error?.errorCategory(),
      stateChangeReason = stateChangeReason,
    )
  }

  override fun cancelStatementExecution(statementId: String): StatementCancellationResponse {
    val cancelQueryExecutionRequest = StopQueryExecutionRequest.builder()
      .queryExecutionId(statementId)
      .build()

    athenaClient.stopQueryExecution(cancelQueryExecutionRequest)
    return StatementCancellationResponse(true)
  }

  override fun buildReportQuery(query: String) =
    if (query.contains("$DATASET_ AS", ignoreCase = true)) query else """$DATASET_ AS ($query)"""

  private fun buildPromptsQuery(prompts: Map<String, String>?): String {
    if (prompts.isNullOrEmpty()) {
      return "WITH $PROMPTS AS (SELECT '' FROM DUAL)"
    }
    val promptsCte = prompts.map { e -> "'${e.value}' AS ${e.key}" }.joinToString(", ")
    return "WITH $PROMPTS AS (SELECT $promptsCte FROM DUAL)"
  }

  private fun buildFinalQuery(
    prompts: String,
    reportQuery: String,
    policiesQuery: String,
    filtersQuery: String,
    selectFromFinalStageQuery: String,
  ): String {
    val query = listOf(prompts, reportQuery, policiesQuery, filtersQuery).joinToString(",") + "\n$selectFromFinalStageQuery"
    RepositoryHelper.log.debug("Database query: $query")
    return query
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

  override fun buildSummaryQuery(query: String, summaryTableId: String): String {
    return """
          CREATE TABLE AwsDataCatalog.reports.$summaryTableId
          WITH (format = 'PARQUET') 
          AS (SELECT * FROM TABLE(system.query(query => '$query')));
    """
  }
}
