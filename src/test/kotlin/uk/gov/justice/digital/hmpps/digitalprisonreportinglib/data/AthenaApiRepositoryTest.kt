package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.oauth2.jwt.Jwt
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.GetQueryExecutionResponse
import software.amazon.awssdk.services.athena.model.QueryExecution
import software.amazon.awssdk.services.athena.model.QueryExecutionContext
import software.amazon.awssdk.services.athena.model.QueryExecutionStatus
import software.amazon.awssdk.services.athena.model.ResultConfiguration
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.StartQueryExecutionResponse
import software.amazon.awssdk.services.athena.model.StopQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.StopQueryExecutionResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.Companion.CONTEXT
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.Companion.FALSE_WHERE_CLAUSE
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.Companion.PROMPT
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.Companion.TRUE_WHERE_CLAUSE
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Datasource
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Report
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy.PolicyResult.POLICY_DENY
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy.PolicyResult.POLICY_PERMIT
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementCancellationResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionStatus
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.TableIdGenerator
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class AthenaApiRepositoryTest {

  companion object {
    val tableId = "_a6227417_bdac_40bb_bc81_49c750daacd7"
    val executionId = "someId"
    val testDb = "testdb"
    val testCatalog = "testcatalog"
    val dpdQuery = "SELECT column_a,column_b FROM schema_a.table_a"
    val defaultDatasetCte = "dataset_ AS (SELECT column_a,column_b FROM schema_a.table_a)"
    val emptyPromptsCte = "$PROMPT AS (SELECT '''' FROM DUAL)"
    private val testUsername = "aUser"
    private val testCaseload = "aCaseload"
    private val testAccountType = "GENERAL"
    private val contextCte = """WITH $CONTEXT AS (
      SELECT 
      ''$testUsername'' AS username, 
      ''$testCaseload'' AS caseload, 
      ''$testAccountType'' AS account_type 
      FROM DUAL
      )"""
  }
  fun sqlStatement(tableId: String, whereClauseCondition: String? = TRUE_WHERE_CLAUSE, promptsCte: String? = emptyPromptsCte, datasetCte: String? = defaultDatasetCte) =
    """          CREATE TABLE AwsDataCatalog.reports.$tableId 
          WITH (
            format = 'PARQUET'
          ) 
          AS (
          SELECT * FROM TABLE(system.query(query =>
           '$contextCte,$promptsCte,$datasetCte,policy_ AS (SELECT * FROM dataset_ WHERE $whereClauseCondition),filter_ AS (SELECT * FROM policy_ WHERE $TRUE_WHERE_CLAUSE)
SELECT *
          FROM filter_ ORDER BY column_a asc'
           )) 
          );
    """.trimIndent()

  private val athenaClient = mock<AthenaClient>()
  private val tableIdGenerator = mock<TableIdGenerator>()
  private val productDefinition = mock<SingleReportProductDefinition>()
  private val athenaApiRepository = AthenaApiRepository(
    athenaClient,
    tableIdGenerator,
  )
  private val startQueryExecutionResponse = mock<StartQueryExecutionResponse>()
  private val dataset = mock<Dataset>()
  private val datasource = mock<Datasource>()
  private val report = mock<Report>()
  private val userToken = mock<DprAuthAwareAuthenticationToken>()

  @ParameterizedTest
  @CsvSource(
    "$POLICY_PERMIT, $TRUE_WHERE_CLAUSE",
    "${POLICY_DENY}, $FALSE_WHERE_CLAUSE",
  )
  fun `executeQueryAsync should call the athena data api with the correct query which includes the context_ cte and return the execution id and table id`(policyEngineResult: String, whereClauseCondition: String) {
    val startQueryExecutionRequest = setupMocks(whereClause = whereClauseCondition)
    whenever(dataset.query).thenReturn(dpdQuery)
    val actual = athenaApiRepository.executeQueryAsync(
      productDefinition = productDefinition,
      filters = emptyList(),
      sortColumn = "column_a",
      sortedAsc = true,
      policyEngineResult = policyEngineResult,
      userToken = userToken,
    )

    assertEquals(StatementExecutionResponse(tableId, executionId), actual)
    verify(athenaClient).startQueryExecution(startQueryExecutionRequest)
  }

  @Test
  fun `executeQueryAsync should map prompts to the prompt_ CTE correctly`() {
    val startQueryExecutionRequest = setupMocks(promptsCte = "$PROMPT AS (SELECT ''filterValue1'' AS filterName1, ''filterValue2'' AS filterName2 FROM DUAL)")
    val prompts = mapOf("filterName1" to "filterValue1", "filterName2" to "filterValue2")
    whenever(dataset.query).thenReturn(defaultDatasetCte)
    val actual = athenaApiRepository.executeQueryAsync(
      productDefinition = productDefinition,
      filters = emptyList(),
      sortColumn = "column_a",
      sortedAsc = true,
      policyEngineResult = POLICY_PERMIT,
      prompts = prompts,
      userToken = userToken,
    )

    assertEquals(StatementExecutionResponse(tableId, executionId), actual)
    verify(athenaClient).startQueryExecution(startQueryExecutionRequest)
  }

  @Test
  fun `executeQueryAsync should use the existing main report query when the dataset_ CTE is already embedded into the query`() {
    val dpdQuery = "dataset_ as (SELECT column_c,column_d FROM schema_a.table_a)"
    val startQueryExecutionRequest = setupMocks(datasetCte = dpdQuery)

    whenever(dataset.query).thenReturn(dpdQuery)
    val actual = athenaApiRepository.executeQueryAsync(
      productDefinition = productDefinition,
      filters = emptyList(),
      sortColumn = "column_a",
      sortedAsc = true,
      policyEngineResult = POLICY_PERMIT,
      userToken = userToken,
    )

    assertEquals(StatementExecutionResponse(tableId, executionId), actual)
    verify(athenaClient).startQueryExecution(startQueryExecutionRequest)
  }

  @ParameterizedTest
  @CsvSource(
    "QUEUED, SUBMITTED",
    "RUNNING, STARTED",
    "SUCCEEDED, FINISHED",
    "CANCELLED, ABORTED",
  )
  fun `getStatementStatus should call the getQueryExecution athena api with the correct statement ID and return the StatementExecutionStatus mapped correctly`(athenaStatus: String, redshiftStatus: String) {
    val query = sqlStatement(tableId = "tableId")
    val statementId = "statementId"
    val getQueryExecutionRequest = GetQueryExecutionRequest.builder()
      .queryExecutionId(statementId)
      .build()
    val completionTime = Instant.now()
    val submissionTime = completionTime.minus(Duration.of(10, ChronoUnit.MINUTES))
    val getQueryExecutionResponse = GetQueryExecutionResponse.builder()
      .queryExecution(
        QueryExecution.builder()
          .query(query)
          .status(
            QueryExecutionStatus.builder().state(
              athenaStatus,
            )
              .submissionDateTime(submissionTime)
              .completionDateTime(completionTime)
              .build(),
          ).build(),
      ).build()
    whenever(
      athenaClient.getQueryExecution(
        getQueryExecutionRequest,
      ),
    ).thenReturn(getQueryExecutionResponse)
    val tenMinutesInNanoseconds = 600000000000
    val expected = StatementExecutionStatus(
      redshiftStatus,
      tenMinutesInNanoseconds,
      0L,
      0L,
    )
    val actual = athenaApiRepository.getStatementStatus(statementId)

    assertEquals(expected, actual)
  }

  @Test
  fun `cancelStatementExecution should call the stopQueryExecution athena api with the correct statement ID and return a successful StatementCancellationResponse`() {
    val statementId = "statementId"
    val stopQueryExecutionRequest = StopQueryExecutionRequest.builder()
      .queryExecutionId(statementId)
      .build()
    val stopQueryExecutionResponse = StopQueryExecutionResponse.builder().build()
    whenever(
      athenaClient.stopQueryExecution(
        stopQueryExecutionRequest,
      ),
    ).thenReturn(stopQueryExecutionResponse)
    val expected = StatementCancellationResponse(true)
    val actual = athenaApiRepository.cancelStatementExecution(statementId)

    assertEquals(expected, actual)
  }

  private fun setupMocks(whereClause: String? = TRUE_WHERE_CLAUSE, promptsCte: String? = emptyPromptsCte, datasetCte: String? = defaultDatasetCte): StartQueryExecutionRequest {
    val queryExecutionContext = QueryExecutionContext.builder()
      .database(testDb)
      .catalog(testCatalog)
      .build()
    val resultConfiguration = ResultConfiguration.builder()
      .outputLocation("s3://dpr-working-development/reports/$tableId/")
      .build()
    val startQueryExecutionRequest = StartQueryExecutionRequest.builder()
      .queryString(
        sqlStatement(
          tableId = tableId,
          whereClauseCondition = whereClause,
          promptsCte = promptsCte,
          datasetCte = datasetCte,
        ),
      )
      .queryExecutionContext(queryExecutionContext)
      .resultConfiguration(resultConfiguration)
      .build()
    whenever(
      tableIdGenerator.generateNewExternalTableId(),
    ).thenReturn(
      tableId,
    )
    whenever(productDefinition.reportDataset).thenReturn(dataset)
    whenever(productDefinition.datasource).thenReturn(datasource)
    whenever(productDefinition.report).thenReturn(report)
    whenever(datasource.database).thenReturn(testDb)
    whenever(datasource.catalog).thenReturn(testCatalog)

    whenever(
      athenaClient.startQueryExecution(
        ArgumentMatchers.any(StartQueryExecutionRequest::class.java),
      ),
    ).thenReturn(startQueryExecutionResponse)

    whenever(
      startQueryExecutionResponse.queryExecutionId(),
    ).thenReturn(executionId)

    val jwt = mock<Jwt>()
    whenever(userToken.jwt).thenReturn(jwt)
    whenever(jwt.subject).thenReturn(testUsername)
    whenever(userToken.getCaseLoads()).thenReturn(listOf(testCaseload))

    return startQueryExecutionRequest
  }
}
