package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
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
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.Companion.FALSE_WHERE_CLAUSE
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.Companion.TRUE_WHERE_CLAUSE
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy.PolicyResult
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementCancellationResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionStatus
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.TableIdGenerator
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class AthenaApiRepositoryTest {

  companion object {
    val dpdQuery = "SELECT column_a,column_b FROM schema_a.table_a"
  }
  fun sqlStatement(tableId: String, whereClauseCondition: String? = TRUE_WHERE_CLAUSE) =
    """          CREATE TABLE AwsDataCatalog.reports.$tableId 
          WITH (
            format = 'PARQUET'
          ) 
          AS (
          SELECT * FROM TABLE(system.query(query =>
           'WITH dataset_ AS ($dpdQuery),policy_ AS (SELECT * FROM dataset_ WHERE $whereClauseCondition),filter_ AS (SELECT * FROM policy_ WHERE $TRUE_WHERE_CLAUSE)
SELECT *
          FROM filter_ ORDER BY column_a asc'
           )) 
          );
    """.trimIndent()

  @ParameterizedTest
  @CsvSource(
    "${PolicyResult.POLICY_PERMIT}, $TRUE_WHERE_CLAUSE",
    "${PolicyResult.POLICY_DENY}, $FALSE_WHERE_CLAUSE",
  )
  fun `executeQueryAsync should call the athena data api with the correct query and return the execution id and table id`(policyEngineResult: String, whereClauseCondition: String) {
    val athenaClient = mock<AthenaClient>()
    val startQueryExecutionResponse = mock<StartQueryExecutionResponse>()
    val tableIdGenerator = mock<TableIdGenerator>()
    val tableId = "_a6227417_bdac_40bb_bc81_49c750daacd7"
    val executionId = "someId"
    val testDb = "testdb"
    val testCatalog = "testcatalog"
    val athenaApiRepository = AthenaApiRepository(
      athenaClient,
      tableIdGenerator,
    )
    val queryExecutionContext = QueryExecutionContext.builder()
      .database(testDb)
      .catalog(testCatalog)
      .build()
    val resultConfiguration = ResultConfiguration.builder()
      .outputLocation("s3://dpr-working-development/reports/$tableId/")
      .build()
    val startQueryExecutionRequest = StartQueryExecutionRequest.builder()
      .queryString(sqlStatement(tableId, whereClauseCondition))
      .queryExecutionContext(queryExecutionContext)
      .resultConfiguration(resultConfiguration)
      .build()
    whenever(
      tableIdGenerator.generateNewExternalTableId(),
    ).thenReturn(
      tableId,
    )

    whenever(
      athenaClient.startQueryExecution(
        startQueryExecutionRequest,
      ),
    ).thenReturn(startQueryExecutionResponse)

    whenever(
      startQueryExecutionResponse.queryExecutionId(),
    ).thenReturn(executionId)

    val actual = athenaApiRepository.executeQueryAsync(
      query = dpdQuery,
      filters = emptyList(),
      sortColumn = "column_a",
      sortedAsc = true,
      policyEngineResult = policyEngineResult,
      database = testDb,
      catalog = testCatalog,
    )

    assertEquals(StatementExecutionResponse(tableId, executionId), actual)
  }

  @ParameterizedTest
  @CsvSource(
    "QUEUED, SUBMITTED",
    "RUNNING, STARTED",
    "SUCCEEDED, FINISHED",
    "CANCELLED, ABORTED",
  )
  fun `getStatementStatus should call the getQueryExecution athena api with the correct statement ID and return the StatementExecutionStatus mapped correctly`(athenaStatus: String, redshiftStatus: String) {
    val athenaClient = mock<AthenaClient>()
    val tableIdGenerator = mock<TableIdGenerator>()
    val athenaApiRepository = AthenaApiRepository(
      athenaClient,
      tableIdGenerator,
    )
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
      query,
      0L,
      0L,
    )
    val actual = athenaApiRepository.getStatementStatus(statementId)

    assertEquals(expected, actual)
  }

  @Test
  fun `cancelStatementExecution should call the stopQueryExecution athena api with the correct statement ID and return a successful StatementCancellationResponse`() {
    val athenaClient = mock<AthenaClient>()
    val tableIdGenerator = mock<TableIdGenerator>()
    val athenaApiRepository = AthenaApiRepository(
      athenaClient,
      tableIdGenerator,
    )
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
}
