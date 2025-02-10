package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.AthenaError
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.QueryExecutionContext
import software.amazon.awssdk.services.athena.model.QueryExecutionStatus
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.StopQueryExecutionRequest
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Datasource
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterType.Date
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportFilter
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportSummary
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementCancellationResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionStatus
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.TableIdGenerator
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Prompt

const val QUERY_STARTED = "STARTED"
const val QUERY_FINISHED = "FINISHED"
const val QUERY_ABORTED = "ABORTED"
const val QUERY_FAILED = "FAILED"

@Service
@Primary
@ConditionalOnBean(AthenaClient::class)
class AthenaApiRepository(
  val athenaClient: AthenaClient,
  val tableIdGenerator: TableIdGenerator,
  @Value("\${dpr.lib.redshiftdataapi.athenaworkgroup:workgroupArn}")
  val athenaWorkgroup: String,
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
  ): StatementExecutionResponse {
    val tableId = tableIdGenerator.generateNewExternalTableId()
    val finalQuery = """
          /* $productDefinitionId $productDefinitionName $reportOrDashboardId $reportOrDashboardName */
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
        buildDatasetQuery(query),
        buildReportQuery(reportFilter),
        buildPolicyQuery(policyEngineResult, determinePreviousCteName(reportFilter)),
        // The filters part will be replaced with the variables CTE
        "$FILTER_ AS (SELECT * FROM $POLICY_ WHERE $TRUE_WHERE_CLAUSE)",
        buildFinalStageQuery(dynamicFilterFieldId, sortColumn, sortedAsc),
      ).replace("'", "''")
    }'
           )) 
          );
    """.trimIndent()

    return executeQueryAsync(datasource, tableId, finalQuery)
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

  private fun buildPromptsQuery(prompts: List<Prompt>?): String {
    if (prompts.isNullOrEmpty()) {
      return "$PROMPT AS (SELECT '' FROM DUAL)"
    }
    val promptsCte = prompts.joinToString(", ") { prompt -> buildPromptsQueryBasedOnType(prompt) }
    return "$PROMPT AS (SELECT $promptsCte FROM DUAL)"
  }

  private fun buildPromptsQueryBasedOnType(prompt: Prompt): String {
    return when (prompt.type) {
      Date -> "TO_DATE('${prompt.value}','yyyy-mm-dd') AS ${prompt.name}"
      else -> "'${prompt.value}' AS ${prompt.name}"
    }
  }

  private fun buildContextQuery(userToken: DprAuthAwareAuthenticationToken?): String =
    """WITH $CONTEXT AS (
      SELECT 
      '${userToken?.jwt?.subject}' AS username, 
      '${userToken?.getActiveCaseLoad()}' AS caseload, 
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
      "RUNNING" to QUERY_STARTED,
      "SUCCEEDED" to QUERY_FINISHED,
      "CANCELLED" to QUERY_ABORTED,
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
