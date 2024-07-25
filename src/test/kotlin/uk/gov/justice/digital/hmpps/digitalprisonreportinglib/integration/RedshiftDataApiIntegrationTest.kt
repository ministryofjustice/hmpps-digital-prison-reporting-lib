package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration

import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.web.util.UriBuilder
import software.amazon.awssdk.services.redshiftdata.model.ActiveStatementsExceededException
import software.amazon.awssdk.services.redshiftdata.model.ValidationException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.DataApiSyncController.FiltersPrefix.RANGE_FILTER_END_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.DataApiSyncController.FiltersPrefix.RANGE_FILTER_START_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ReportDefinitionController
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.Count
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementCancellationResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionStatus
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.ConfiguredApiService

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RedshiftDataApiIntegrationTest : IntegrationTestBase() {
  @MockBean
  private lateinit var configuredApiService: ConfiguredApiService

  @MockBean
  lateinit var productDefinitionRepository: ProductDefinitionRepository

  @Test
  fun `Calling the async execute statement endpoint calls the configuredApiService with the correct arguments`() {
    val queryExecutionId = "queryExecutionId"
    val tableId = "tableId"
    val statementExecutionResponse = StatementExecutionResponse(tableId, queryExecutionId)
    val filtersPrefix = "filters."
    val dateStartFilter = "date$RANGE_FILTER_START_SUFFIX"
    val dateEndFilter = "date$RANGE_FILTER_END_SUFFIX"
    val startDate = "2024-02-20"
    val endDate = "2024-02-22"
    given(
      configuredApiService.validateAndExecuteStatementAsync(
        eq("external-movements"),
        eq("last-month"),
        eq(mapOf(dateStartFilter to startDate, dateEndFilter to endDate)),
        eq("date"),
        eq(false),
        any<DprAuthAwareAuthenticationToken>(),
        eq(null),
        eq(null),
        eq("definitions/prisons/orphanage"),
      ),
    )
      .willReturn(statementExecutionResponse)

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/async/reports/external-movements/last-month")
          .queryParam(filtersPrefix + dateStartFilter, startDate)
          .queryParam(filtersPrefix + dateEndFilter, endDate)
          .queryParam("sortColumn", "date")
          .queryParam("sortedAsc", false)
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody()
      .json(
        """{
          "tableId": "$tableId",
          "executionId": "$queryExecutionId"
        }
      """,
      )
  }

  @Test
  fun `When a ValidationException is thrown the async execute statement endpoint responds with 400`() {
    given(
      configuredApiService.validateAndExecuteStatementAsync(
        eq("external-movements"),
        eq("last-month"),
        eq(emptyMap()),
        eq("date"),
        eq(false),
        any<DprAuthAwareAuthenticationToken>(),
        eq(null),
        eq(null),
        eq("definitions/prisons/orphanage"),
      ),
    )
      .willThrow(ValidationException.builder().message("Validation Error").build())

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/async/reports/external-movements/last-month")
          .queryParam("sortColumn", "date")
          .queryParam("sortedAsc", false)
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @Test
  fun `When an ActiveStatementsExceededException is thrown the async execute statement endpoint responds with 429`() {
    given(
      configuredApiService.validateAndExecuteStatementAsync(
        eq("external-movements"),
        eq("last-month"),
        eq(emptyMap()),
        eq("date"),
        eq(false),
        any<DprAuthAwareAuthenticationToken>(),
        eq(null),
        eq(null),
        eq("definitions/prisons/orphanage"),
      ),
    )
      .willThrow(ActiveStatementsExceededException.builder().build())

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/async/reports/external-movements/last-month")
          .queryParam("sortColumn", "date")
          .queryParam("sortedAsc", false)
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isEqualTo(429)
  }

  @Test
  fun `Calling the report status endpoint calls the getStatementStatus of the ConfiguredApiService with the correct arguments`() {
    val queryExecutionId = "queryExecutionId"
    val reportId = "external-movements"
    val reportVariantId = "last-month"
    val status = "FINISHED"
    val duration = 278109264L
    val resultRows = 10L
    val resultSize = 100L
    val statementExecutionStatus = StatementExecutionStatus(
      status,
      duration,
      resultRows,
      resultSize,
    )
    given(
      configuredApiService.getStatementStatus(
        eq(queryExecutionId),
        eq(reportId),
        eq(reportVariantId),
        eq(ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE),
      ),
    )
      .willReturn(statementExecutionStatus)

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/reports/$reportId/$reportVariantId/statements/$queryExecutionId/status")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody()
      .json(
        """{
          "status": "$status",
          "duration": $duration,
          "resultRows": $resultRows,
          "resultSize": $resultSize,
          "error": null
        }
      """,
      )
  }

  @Test
  fun `Calling the report cancellation endpoint calls the cancelStatementExecution of the ConfiguredApiService with the correct arguments`() {
    val queryExecutionId = "queryExecutionId"
    val reportId = "external-movements"
    val reportVariantId = "last-month"
    val statementCancellationResponse = StatementCancellationResponse(
      true,
    )
    given(
      configuredApiService.cancelStatementExecution(
        eq(queryExecutionId),
        eq(reportId),
        eq(reportVariantId),
        eq(ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE),
      ),
    )
      .willReturn(statementCancellationResponse)

    webTestClient.delete()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/reports/$reportId/$reportVariantId/statements/$queryExecutionId")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody()
      .json(
        """{
          "cancellationSucceeded": true
        }
      """,
      )
  }

  @Test
  fun `Calling the getStatementResult endpoint calls the configuredApiService with the correct arguments`() {
    val tableId = "tableId"
    val selectedPage = 2L
    val pageSize = 20L
    val expectedServiceResult =
      listOf(
        mapOf(
          "prisonNumber" to "1",
          "name" to "FirstName",
          "date" to "2023-05-20",
          "origin" to "OriginLocation",
          "destination" to "DestinationLocation",
          "direction" to "in",
          "type" to "trn",
          "reason" to "normal transfer",
        ),
      )

    given(
      configuredApiService.getStatementResult(
        eq(tableId),
        eq("external-movements"),
        eq("last-month"),
        eq(ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE),
        eq(selectedPage),
        eq(pageSize),
      ),
    )
      .willReturn(expectedServiceResult)

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/reports/external-movements/last-month/tables/$tableId/result")
          .queryParam("selectedPage", 2L)
          .queryParam("pageSize", 20L)
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody()
      .json(Gson().toJson(expectedServiceResult))
  }

  @Test
  fun `Calling the getSummaryResult endpoint calls the configuredApiService with the correct arguments`() = runTest {
    val tableId = "tableId"
    val summaryId = "summaryId"
    val expectedServiceResult =
      listOf(
        mapOf(
          "total" to "10",
        ),
      )

    given(
      configuredApiService.getSummaryResult(
        eq(tableId),
        eq(summaryId),
        eq("external-movements"),
        eq("last-month"),
        eq(ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE),
      ),
    )
      .willReturn(expectedServiceResult)

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/reports/external-movements/last-month/tables/$tableId/result/summary/$summaryId")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody()
      .json(Gson().toJson(expectedServiceResult))
  }

  @Test
  fun `Calling the getExternalTableRowCount endpoint calls the configuredApiService with the correct arguments`() {
    val tableId = "tableId"
    val expectedServiceResult = Count(5)

    given(
      configuredApiService.count(
        eq(tableId),
      ),
    )
      .willReturn(Count(5))

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/report/tables/$tableId/count")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody()
      .json(Gson().toJson(expectedServiceResult))
  }
}
