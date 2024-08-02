package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.AthenaError
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.QueryExecutionContext
import software.amazon.awssdk.services.athena.model.QueryExecutionStatus
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.StopQueryExecutionRequest
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Datasource
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementCancellationResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionStatus
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.TableIdGenerator

@Service
class AthenaApiRepository(
  val athenaClient: AthenaClient,
  val tableIdGenerator: TableIdGenerator,
  @Value("\${dpr.lib.redshiftdataapi.athenaworkgroup:workgroupArn}")
  private val athenaWorkgroup: String,
) : AthenaAndRedshiftCommonRepository() {

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
    val finalQuery = """
          /* ${productDefinition.id} ${productDefinition.name} ${productDefinition.report.id} ${productDefinition.report.name} */
          CREATE TABLE AwsDataCatalog.reports.$tableId 
          WITH (
            format = 'PARQUET'
          ) 
          AS (
          SELECT * FROM TABLE(system.query(query =>
           '${
      buildFinalQuery(
        buildContextQuery(userToken),
        buildPromptsQuery(prompts),
        buildDatasetQuery(productDefinition.reportDataset.query),
        buildReportQuery(productDefinition.report.filter),
        buildPolicyQuery(policyEngineResult, determinePreviousCteName(productDefinition)),
        // The filters part will be replaced with the variables CTE
        "$FILTER_ AS (SELECT * FROM $POLICY_ WHERE $TRUE_WHERE_CLAUSE)",
        buildFinalStageQuery(dynamicFilterFieldId, sortColumn, sortedAsc),
      ).replace("'", "''")
    }'
           )) 
          );
    """.trimIndent()

    return executeQueryAsync(productDefinition.datasource, tableId, finalQuery)
  }

  override fun executeQueryAsync(
    datasource: Datasource,
    tableId: String,
    query: String,
  ): StatementExecutionResponse {
    val queryExecutionContext = QueryExecutionContext.builder()
      .database(datasource.database)
      .catalog(datasource.catalog)
      .build()
    val startQueryExecutionRequest = StartQueryExecutionRequest.builder()
      .queryString(query)
      .queryExecutionContext(queryExecutionContext)
      .workGroup(athenaWorkgroup)
      .build()
    log.debug("Full async query: {}", query)
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

  override fun buildDatasetQuery(query: String) =
    if (query.contains("$DATASET_ AS", ignoreCase = true)) query else """$DATASET_ AS ($query)"""

  private fun buildPromptsQuery(prompts: Map<String, String>?): String {
    if (prompts.isNullOrEmpty()) {
      return "$PROMPT AS (SELECT '' FROM DUAL)"
    }
    val promptsCte = prompts.map { e -> "'${e.value}' AS ${e.key}" }.joinToString(", ")
    return "$PROMPT AS (SELECT $promptsCte FROM DUAL)"
  }

  private fun buildContextQuery(userToken: DprAuthAwareAuthenticationToken?): String =
    """WITH $CONTEXT AS (
      SELECT 
      '${userToken?.jwt?.subject}' AS username, 
      '${userToken?.getCaseLoads()?.first()}' AS caseload, 
      'GENERAL' AS account_type 
      FROM DUAL
      )"""

  private fun buildFinalQuery(
    context: String,
    prompts: String,
    datasetQuery: String,
    reportQuery: String,
    policiesQuery: String,
    filtersQuery: String,
    selectFromFinalStageQuery: String,
  ): String {
    val query = listOf(context, prompts, datasetQuery, reportQuery, policiesQuery, filtersQuery).joinToString(",") + "\n$selectFromFinalStageQuery"
    log.debug("Database query: $query")
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
}
