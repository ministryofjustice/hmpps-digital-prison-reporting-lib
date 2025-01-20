package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration

import com.google.gson.Gson
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.jdbc.UncategorizedSQLException
import org.springframework.web.util.UriBuilder
import software.amazon.awssdk.services.redshiftdata.model.ActiveStatementsExceededException
import software.amazon.awssdk.services.redshiftdata.model.ValidationException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.DataApiSyncController
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.DataApiSyncController.FiltersPrefix.RANGE_FILTER_END_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.DataApiSyncController.FiltersPrefix.RANGE_FILTER_START_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ReportDefinitionController
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.Count
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementCancellationResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionStatus
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.AsyncDataApiService
import java.sql.SQLException

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RedshiftDataApiIntegrationTest : IntegrationTestBase() {
  @MockBean
  private lateinit var asyncDataApiService: AsyncDataApiService

  @MockBean
  lateinit var productDefinitionRepository: ProductDefinitionRepository

  @Test
  fun `Calling the report async execute statement endpoint calls the asyncDataApiService with the correct arguments`() {
    val queryExecutionId = "queryExecutionId"
    val tableId = "tableId"
    val statementExecutionResponse = StatementExecutionResponse(tableId, queryExecutionId)
    val filtersPrefix = "filters."
    val dateStartFilter = "date$RANGE_FILTER_START_SUFFIX"
    val dateEndFilter = "date$RANGE_FILTER_END_SUFFIX"
    val startDate = "2024-02-20"
    val endDate = "2024-02-22"
    given(
      asyncDataApiService.validateAndExecuteStatementAsync(
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
  fun `Calling the dashboard async execute statement endpoint calls the asyncDataApiService with the correct arguments`() {
    val queryExecutionId = "queryExecutionId"
    val tableId = "tableId"
    val statementExecutionResponse = StatementExecutionResponse(tableId, queryExecutionId)
    given(
      asyncDataApiService.validateAndExecuteStatementAsync(
        reportId = eq("some-metrics-dpd"),
        dashboardId = eq("some-dashboard-id"),
        userToken = any<DprAuthAwareAuthenticationToken>(),
        dataProductDefinitionsPath = eq("definitions/prisons/orphanage"),
        filters = eq(emptyMap()),
      ),
    )
      .willReturn(statementExecutionResponse)

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/async/dashboards/some-metrics-dpd/some-dashboard-id")
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
      asyncDataApiService.validateAndExecuteStatementAsync(
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
      asyncDataApiService.validateAndExecuteStatementAsync(
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
      asyncDataApiService.getStatementStatus(
        eq(queryExecutionId),
        eq(reportId),
        eq(reportVariantId),
        any<DprAuthAwareAuthenticationToken>(),
        eq(ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE),
        anyOrNull(),
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
  fun `Calling the statement status endpoint calls the getStatementStatus of the ConfiguredApiService with the correct arguments`() {
    val queryExecutionId = "queryExecutionId"
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
      asyncDataApiService.getStatementStatus(
        eq(queryExecutionId),
        anyOrNull(),
      ),
    )
      .willReturn(statementExecutionStatus)

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/statements/$queryExecutionId/status")
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
      asyncDataApiService.cancelStatementExecution(
        eq(queryExecutionId),
        eq(reportId),
        eq(reportVariantId),
        any<DprAuthAwareAuthenticationToken>(),
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
  fun `Calling the statement cancellation endpoint calls the cancelStatementExecution of the ConfiguredApiService with the correct arguments`() {
    val queryExecutionId = "queryExecutionId"
    val statementCancellationResponse = StatementCancellationResponse(
      true,
    )
    given(
      asyncDataApiService.cancelStatementExecution(
        eq(queryExecutionId),
      ),
    )
      .willReturn(statementCancellationResponse)

    webTestClient.delete()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/statements/$queryExecutionId")
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
  fun `Calling the report getStatementResult endpoint calls the configuredApiService with the correct arguments`() {
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
      asyncDataApiService.getStatementResult(
        tableId = eq(tableId),
        reportId = eq("external-movements"),
        reportVariantId = eq("last-month"),
        dataProductDefinitionsPath = eq(ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE),
        selectedPage = eq(selectedPage),
        pageSize = eq(pageSize),
        filters = eq(emptyMap()),
        sortedAsc = eq(false),
        sortColumn = eq(null),
        userToken = any<DprAuthAwareAuthenticationToken>(),
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
  fun `Calling the report getStatementResult endpoint with filters and sorting calls the configuredApiService with the correct arguments`() {
    val tableId = "tableId"
    val selectedPage = 2L
    val pageSize = 20L
    val sortColumn = "columnA"
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

    given(asyncDataApiService.getStatementResult(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
      .willReturn(expectedServiceResult)

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/reports/external-movements/last-month/tables/$tableId/result")
          .queryParam("selectedPage", 2L)
          .queryParam("pageSize", 20L)
          .queryParam("sortColumn", sortColumn)
          .queryParam("sortedAsc", true)
          .queryParam("${DataApiSyncController.FiltersPrefix.FILTERS_PREFIX}direction", "out")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody()
      .json(Gson().toJson(expectedServiceResult))

    verify(asyncDataApiService)
      .getStatementResult(
        eq(tableId),
        eq("external-movements"),
        eq("last-month"),
        eq(ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE),
        eq(selectedPage),
        eq(pageSize),
        eq(mapOf("direction" to "out")),
        eq(true),
        eq(sortColumn),
        any<DprAuthAwareAuthenticationToken>(),
      )
  }

  @Test
  fun `The getStatementResult endpoint returns 404 when the redshift table is not found`() {
    val tableId = "tableId"
    val selectedPage = 2L
    val pageSize = 20L

    given(
      asyncDataApiService.getStatementResult(
        tableId = eq(tableId),
        reportId = eq("external-movements"),
        reportVariantId = eq("last-month"),
        dataProductDefinitionsPath = eq(ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE),
        selectedPage = eq(selectedPage),
        pageSize = eq(pageSize),
        filters = eq(emptyMap()),
        sortedAsc = eq(false),
        sortColumn = eq(null),
        userToken = any<DprAuthAwareAuthenticationToken>(),
      ),
    )
      .willThrow(UncategorizedSQLException("EntityNotFoundException from glue - Entity Not Found", "", SQLException()))

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
      .isNotFound
  }

  @Test
  fun `Calling the dashboard getStatementResult endpoint calls the configuredApiService with the correct arguments`() {
    val tableId = "tableId"
    val dpdId = "external-movements"
    val dashboardId = "dashboardId"
    val selectedPage = 2L
    val pageSize = 20L
    val expectedServiceResult =
      listOf(
        mapOf(
          "establishment_id" to "KMI",
          "has_ethnicity" to "10",
          "ethnicity_is_missing" to "30",
        ),
      )

    given(asyncDataApiService.getDashboardStatementResult(any(), any(), any(), any(), any(), any(), any(), any()))
      .willReturn(expectedServiceResult)

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("reports/$dpdId/dashboards/$dashboardId/tables/$tableId/result")
          .queryParam("selectedPage", selectedPage)
          .queryParam("pageSize", pageSize)
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody()
      .json(Gson().toJson(expectedServiceResult))

    verify(asyncDataApiService).getDashboardStatementResult(
      eq(tableId),
      eq(dpdId),
      eq(dashboardId),
      eq(ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE),
      eq(selectedPage),
      eq(pageSize),
      eq(emptyMap()),
      any<DprAuthAwareAuthenticationToken>(),
    )
  }

  @Test
  fun `Calling the dashboard getStatementResult endpoint with filters calls the configuredApiService with the correct arguments`() {
    val tableId = "tableId"
    val dpdId = "external-movements"
    val dashboardId = "dashboardId"
    val selectedPage = 2L
    val pageSize = 20L
    val expectedServiceResult =
      listOf(
        mapOf(
          "establishment_id" to "KMI",
          "has_ethnicity" to "10",
          "ethnicity_is_missing" to "30",
        ),
      )

    given(asyncDataApiService.getDashboardStatementResult(any(), any(), any(), any(), any(), any(), any(), any()))
      .willReturn(expectedServiceResult)

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("reports/$dpdId/dashboards/$dashboardId/tables/$tableId/result")
          .queryParam("selectedPage", selectedPage)
          .queryParam("pageSize", pageSize)
          .queryParam("${DataApiSyncController.FiltersPrefix.FILTERS_PREFIX}direction", "out")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody()
      .json(Gson().toJson(expectedServiceResult))

    verify(asyncDataApiService).getDashboardStatementResult(
      eq(tableId),
      eq(dpdId),
      eq(dashboardId),
      eq(ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE),
      eq(selectedPage),
      eq(pageSize),
      eq(mapOf("direction" to "out")),
      any<DprAuthAwareAuthenticationToken>(),
    )
  }

  @Test
  fun `The dashboard getStatementResult endpoint returns 404 when the redshift table is not found`() {
    val tableId = "tableId"
    val dpdId = "external-movements"
    val dashboardId = "dashboardId"
    val selectedPage = 2L
    val pageSize = 20L

    given(asyncDataApiService.getDashboardStatementResult(any(), any(), any(), any(), any(), any(), any(), any()))
      .willThrow(UncategorizedSQLException("EntityNotFoundException from glue - Entity Not Found", "", SQLException()))

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("reports/$dpdId/dashboards/$dashboardId/tables/$tableId/result")
          .queryParam("selectedPage", selectedPage)
          .queryParam("pageSize", pageSize)
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isNotFound

    verify(asyncDataApiService).getDashboardStatementResult(
      eq(tableId),
      eq(dpdId),
      eq(dashboardId),
      eq(ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE),
      eq(selectedPage),
      eq(pageSize),
      eq(emptyMap()),
      any<DprAuthAwareAuthenticationToken>(),
    )
  }

  @Test
  fun `Calling the getSummaryResult endpoint calls the configuredApiService with the correct arguments`() {
    val tableId = "tableId"
    val summaryId = "summaryId"
    val expectedServiceResult =
      listOf(
        mapOf(
          "total" to "10",
        ),
      )

    given(
      asyncDataApiService.getSummaryResult(
        eq(tableId),
        eq(summaryId),
        eq("external-movements"),
        eq("last-month"),
        eq(ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE),
        eq(emptyMap()),
        any<DprAuthAwareAuthenticationToken>(),
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
      asyncDataApiService.count(
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

  @Test
  fun `Calling the getInteractiveExternalTableRowCount endpoint with filters calls the configuredApiService with the correct arguments`() {
    val tableId = "tableId"
    val expectedServiceResult = Count(10)

    given(asyncDataApiService.count(any(), any(), any(), any(), any(), any()))
      .willReturn(expectedServiceResult)

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/reports/external-movements/last-month/tables/$tableId/count")
          .queryParam("${DataApiSyncController.FiltersPrefix.FILTERS_PREFIX}direction", "out")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody()
      .json(Gson().toJson(expectedServiceResult))

    verify(asyncDataApiService)
      .count(
        eq(tableId),
        eq("external-movements"),
        eq("last-month"),
        eq(mapOf("direction" to "out")),
        any<DprAuthAwareAuthenticationToken>(),
        eq(ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE),
      )
  }
}
