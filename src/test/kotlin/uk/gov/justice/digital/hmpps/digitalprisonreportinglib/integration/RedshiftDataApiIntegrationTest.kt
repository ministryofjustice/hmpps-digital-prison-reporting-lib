package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration

import com.google.gson.Gson
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.web.util.UriBuilder
import software.amazon.awssdk.services.redshiftdata.model.ActiveStatementsExceededException
import software.amazon.awssdk.services.redshiftdata.model.ValidationException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ReportDefinitionController
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.StatementExecutionStatus
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
      .willReturn(queryExecutionId)

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
      .isOk()
      .expectBody(String::class.java)
      .isEqualTo(queryExecutionId)
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
    val status = "FINISHED"
    val duration = 278109264L
    val query = "SELECT * FROM datamart.domain.movement_movement limit 10;"
    val resultRows = 10L
    val statementExecutionStatus = StatementExecutionStatus(
      status,
      duration,
      query,
      resultRows,
    )
    given(
      configuredApiService.getStatementStatus(
        eq(queryExecutionId),
      ),
    )
      .willReturn(statementExecutionStatus)

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/report/statements/$queryExecutionId/status")
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
          "queryString": "$query",
          "resultRows": $resultRows,
          "error": null
        }
      """,
      )
  }

  @Test
  fun `Calling the getStatementResult endpoint calls the configuredApiService with the correct arguments`() {
    val queryExecutionId = "queryExecutionId"
    val expectedServiceResult = listOf(
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
        eq(queryExecutionId),
        eq("external-movements"),
        eq("last-month"),
        eq(ReportDefinitionController.DATA_PRODUCT_DEFINITIONS_PATH_EXAMPLE),
      ),
    )
      .willReturn(expectedServiceResult)

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/reports/external-movements/last-month/statements/$queryExecutionId/result")
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
