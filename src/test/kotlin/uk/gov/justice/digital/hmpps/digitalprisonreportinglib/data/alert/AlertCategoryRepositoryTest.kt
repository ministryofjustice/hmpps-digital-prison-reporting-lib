package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.alert

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.Datum
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.GetQueryExecutionResponse
import software.amazon.awssdk.services.athena.model.GetQueryResultsRequest
import software.amazon.awssdk.services.athena.model.GetQueryResultsResponse
import software.amazon.awssdk.services.athena.model.QueryExecution
import software.amazon.awssdk.services.athena.model.QueryExecutionContext
import software.amazon.awssdk.services.athena.model.QueryExecutionStatus
import software.amazon.awssdk.services.athena.model.ResultSet
import software.amazon.awssdk.services.athena.model.Row
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.StartQueryExecutionResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.alert.AlertCategoryRepository.Companion.ALERT_CATEGORY_QUERY
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.alert.AlertCategoryRepository.Companion.DIGITAL_PRISON_REPORTING_DB
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.alert.AlertCategoryRepository.Companion.NOMIS_CATALOG
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.TableIdGenerator
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class AlertCategoryRepositoryTest {

  val athenaClient = mock<AthenaClient>()
  val tableIdGenerator = mock<TableIdGenerator>()
  val statementId = "statementId"
  val athenaWorkgroup = "workgroup-1"
  val alertCategoryRepository = AlertCategoryRepository(athenaClient, tableIdGenerator, athenaWorkgroup)

  @Test
  fun `should execute the statement, poll for the status and get the results`() {
    setupMocksForStartQueryExecution(athenaWorkgroup, athenaClient, statementId)
    setupMocksForGetStatus(statementId, athenaClient, "SUCCEEDED")

    val row1 = buildRow("domain", "CODE", "DESCRIPTION")
    val row2 = buildRow("ALERT", "B", "End of Custody Temporary Release")

    val getQueryResultsRequest: GetQueryResultsRequest =
      GetQueryResultsRequest.builder()
        .queryExecutionId(statementId)
        .build()
    val getQueryResultsResponse: GetQueryResultsResponse = GetQueryResultsResponse
      .builder()
      .resultSet(ResultSet.builder().rows(listOf(row1, row2)).build())
      .build()

    whenever(
      athenaClient.getQueryResults(
        getQueryResultsRequest,
      ),
    ).thenReturn(getQueryResultsResponse)

    val expected = listOf(
      AlertCategory("ALERT", "B", "End of Custody Temporary Release"),
    )

    val executeStatementWaitAndGetResult = alertCategoryRepository.executeStatementWaitAndGetResult()

    assertEquals(expected, executeStatementWaitAndGetResult)
  }

  private fun setupMocksForGetStatus(statementId: String, athenaClient: AthenaClient, status: String) {
    val getQueryExecutionRequest = GetQueryExecutionRequest.builder()
      .queryExecutionId(statementId)
      .build()
    val completionTime = Instant.now()
    val submissionTime = completionTime.minus(Duration.of(10, ChronoUnit.MINUTES))
    val getQueryExecutionResponse = GetQueryExecutionResponse.builder()
      .queryExecution(
        QueryExecution.builder()
          .query(ALERT_CATEGORY_QUERY)
          .status(
            QueryExecutionStatus.builder().state(
              status,
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
  }

  private fun setupMocksForStartQueryExecution(
    athenaWorkgroup: String,
    athenaClient: AthenaClient,
    statementId: String,
  ) {
    val queryExecutionContext = QueryExecutionContext.builder()
      .database(DIGITAL_PRISON_REPORTING_DB)
      .catalog(NOMIS_CATALOG)
      .build()
    val startQueryExecutionResponse = mock<StartQueryExecutionResponse>()
    val startQueryExecutionRequest = StartQueryExecutionRequest.builder()
      .queryString(
        ALERT_CATEGORY_QUERY,
      )
      .queryExecutionContext(queryExecutionContext)
      .workGroup(athenaWorkgroup)
      .build()
    whenever(
      athenaClient.startQueryExecution(
        eq(startQueryExecutionRequest),
      ),
    ).thenReturn(startQueryExecutionResponse)
    whenever(
      startQueryExecutionResponse.queryExecutionId(),
    ).thenReturn(statementId)
  }

  private fun buildRow(column1: String, column2: String, column3: String): Row? = Row.builder().data(
    listOf(
      Datum.builder().varCharValue(column1).build(),
      Datum.builder().varCharValue(column2).build(),
      Datum.builder().varCharValue(column3).build(),
    ),
  ).build()
}
