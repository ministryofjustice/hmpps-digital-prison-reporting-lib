package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.establishmentsAndWings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
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
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.AthenaReferenceDataRepository.Companion.DIGITAL_PRISON_REPORTING_DB
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.AthenaReferenceDataRepository.Companion.NOMIS_CATALOG
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.QUERY_FAILED
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.establishmentsAndWings.EstablishmentsToWingsRepository.Companion.ESTABLISHMENTS_TO_WINGS_QUERY
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.TableIdGenerator
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class EstablishmentsToWingsRepositoryTest {

  @Test
  fun `should execute the statement, poll for the status and get the results`() {
    val athenaClient = mock<AthenaClient>()
    val tableIdGenerator = mock<TableIdGenerator>()
    val statementId = "statementId"
    val athenaWorkgroup = "workgroup-1"
    val establishmentsToWingsRepository = EstablishmentsToWingsRepository(athenaClient, tableIdGenerator, athenaWorkgroup)
    setupMocksForStartQueryExecution(athenaWorkgroup, athenaClient, statementId)
    setupMocksForGetStatus(statementId, athenaClient, "SUCCEEDED")

    val row1 = buildRow("establishment_code", "establishment_name", "wing")
    val row2 = buildRow("AKI", "ACKLINGTON (HMP)", "AKI-L")
    val row3 = buildRow("AKI", "ACKLINGTON (HMP)", "AKI-C")
    val row4 = buildRow("BFI", "BEDFORD (HMP)", "BFI-D")
    val row5 = buildRow("BFI", "BEDFORD (HMP)", "BFI-E")
    val getQueryResultsRequest: GetQueryResultsRequest =
      GetQueryResultsRequest.builder()
        .queryExecutionId(statementId)
        .build()
    val getQueryResultsResponse: GetQueryResultsResponse = GetQueryResultsResponse
      .builder()
      .resultSet(ResultSet.builder().rows(listOf(row1, row2, row3, row4, row5)).build())
      .build()

    whenever(
      athenaClient.getQueryResults(
        getQueryResultsRequest,
      ),
    ).thenReturn(getQueryResultsResponse)

    val expected = mapOf(
      "AKI" to listOf(
        EstablishmentToWing("AKI", "ACKLINGTON (HMP)", "AKI-L"),
        EstablishmentToWing("AKI", "ACKLINGTON (HMP)", "AKI-C"),
      ),
      "BFI" to listOf(
        EstablishmentToWing("BFI", "BEDFORD (HMP)", "BFI-D"),
        EstablishmentToWing("BFI", "BEDFORD (HMP)", "BFI-E"),
      ),
    )

    val executeStatementWaitAndGetResult = establishmentsToWingsRepository.executeStatementWaitAndGetResult()

    assertEquals(expected, executeStatementWaitAndGetResult)
  }

  @Test
  fun `should aggregate the results from all pages`() {
    val athenaClient = mock<AthenaClient>()
    val tableIdGenerator = mock<TableIdGenerator>()
    val statementId = "statementId"
    val athenaWorkgroup = "workgroup-1"
    val establishmentsToWingsRepository = EstablishmentsToWingsRepository(athenaClient, tableIdGenerator, athenaWorkgroup)
    setupMocksForStartQueryExecution(athenaWorkgroup, athenaClient, statementId)
    setupMocksForGetStatus(statementId, athenaClient, "SUCCEEDED")

    val row1 = buildRow("establishment_code", "establishment_name", "wing")
    val row2 = buildRow("AKI", "ACKLINGTON (HMP)", "AKI-L")
    val row3 = buildRow("AKI", "ACKLINGTON (HMP)", "AKI-C")
    val row4 = buildRow("BFI", "BEDFORD (HMP)", "BFI-D")
    val row5 = buildRow("BFI", "BEDFORD (HMP)", "BFI-E")
    val page2Row2 = buildRow("AKI", "ACKLINGTON (HMP)", "AKI-B")
    val getQueryResultsRequestBuilder: GetQueryResultsRequest.Builder =
      GetQueryResultsRequest.builder()
        .queryExecutionId(statementId)
    val getQueryResultsResponse1: GetQueryResultsResponse = GetQueryResultsResponse
      .builder()
      .resultSet(ResultSet.builder().rows(listOf(row1, row2, row3, row4, row5)).build())
      .nextToken("page2")
      .build()
    val getQueryResultsResponse2: GetQueryResultsResponse = GetQueryResultsResponse
      .builder()
      .resultSet(ResultSet.builder().rows(listOf(page2Row2)).build())
      .build()

    whenever(
      athenaClient.getQueryResults(
        getQueryResultsRequestBuilder.build(),
      ),
    ).thenReturn(getQueryResultsResponse1)

    whenever(
      athenaClient.getQueryResults(
        getQueryResultsRequestBuilder.nextToken("page2").build(),
      ),
    ).thenReturn(getQueryResultsResponse2)

    val expected = mapOf(
      "AKI" to listOf(
        EstablishmentToWing("AKI", "ACKLINGTON (HMP)", "AKI-L"),
        EstablishmentToWing("AKI", "ACKLINGTON (HMP)", "AKI-C"),
        EstablishmentToWing("AKI", "ACKLINGTON (HMP)", "AKI-B"),
      ),
      "BFI" to listOf(
        EstablishmentToWing("BFI", "BEDFORD (HMP)", "BFI-D"),
        EstablishmentToWing("BFI", "BEDFORD (HMP)", "BFI-E"),
      ),
    )

    val executeStatementWaitAndGetResult = establishmentsToWingsRepository.executeStatementWaitAndGetResult()

    assertEquals(expected, executeStatementWaitAndGetResult)
  }

  @Test
  fun `should return empty map if the status is not successful`() {
    val athenaClient = mock<AthenaClient>()
    val tableIdGenerator = mock<TableIdGenerator>()
    val statementId = "statementId"
    val athenaWorkgroup = "workgroup-1"
    val establishmentsToWingsRepository = EstablishmentsToWingsRepository(athenaClient, tableIdGenerator, athenaWorkgroup)
    setupMocksForStartQueryExecution(athenaWorkgroup, athenaClient, statementId)
    setupMocksForGetStatus(statementId, athenaClient, QUERY_FAILED)

    val executeStatementWaitAndGetResult = establishmentsToWingsRepository.executeStatementWaitAndGetResult()

    verify(athenaClient, times(0)).getQueryResults(
      any(GetQueryResultsRequest::class.java),
    )

    assertEquals(mapOf<String, List<EstablishmentToWing>>(), executeStatementWaitAndGetResult)
  }

  private fun buildRow(column1: String, column2: String, column3: String): Row? = Row.builder().data(
    listOf(
      Datum.builder().varCharValue(column1).build(),
      Datum.builder().varCharValue(column2).build(),
      Datum.builder().varCharValue(column3).build(),
    ),
  ).build()

  private fun setupMocksForGetStatus(statementId: String, athenaClient: AthenaClient, status: String) {
    val getQueryExecutionRequest = GetQueryExecutionRequest.builder()
      .queryExecutionId(statementId)
      .build()
    val completionTime = Instant.now()
    val submissionTime = completionTime.minus(Duration.of(10, ChronoUnit.MINUTES))
    val getQueryExecutionResponse = GetQueryExecutionResponse.builder()
      .queryExecution(
        QueryExecution.builder()
          .query(ESTABLISHMENTS_TO_WINGS_QUERY)
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
        ESTABLISHMENTS_TO_WINGS_QUERY,
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
}
