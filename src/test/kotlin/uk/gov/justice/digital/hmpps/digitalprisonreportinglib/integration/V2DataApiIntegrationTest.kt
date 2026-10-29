package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration

import com.google.gson.Gson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.StatusAssertions
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.ExecutionContext
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardExecutionResults
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.MetricData
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ReportExecutionResults

class V2DataApiIntegrationTest : IntegrationTestBase() {
  companion object {
    fun dateTimeWithSeconds(dateTime: Any?) = """$dateTime:00"""

    @JvmStatic
    @DynamicPropertySource
    fun registerProperties(registry: DynamicPropertyRegistry) {
      registry.add("dpr.lib.definition.locations") { "productDefinitionWithFormula.json" }
    }
  }

  @Test
  fun `should return data inside results object for sync report data endpoint`() {
    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/v2/reports/external-movements/last-month")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody(ReportExecutionResults::class.java)
      .value {
        assertThat(it!!.data).isNotEmpty()
        assertThat(it.policyBasedDataChanges).isEmpty()
      }
  }

  @Test
  fun `should return data inside results object for sync dashboard data endpoint`() {
    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/v2/reports/external-movements/dashboards/age-breakdown-dashboard-1")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody(DashboardExecutionResults::class.java)
      .value {
        assertThat(it!!.data).isNotEmpty()
        assertThat(it.policyBasedDataChanges).isEmpty()
      }
  }

  @Test
  fun `should return data inside results object for async report data endpoint`() {
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
        selectedPage = eq(selectedPage),
        pageSize = eq(pageSize),
        filters = eq(emptyMap()),
        executionContext = any<ExecutionContext>(),
        sortedAsc = eq(null),
        sortColumn = eq(null),
      ),
    )
      .willReturn(expectedServiceResult)

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/v2/reports/external-movements/last-month/tables/$tableId/result")
          .queryParam("selectedPage", 2L)
          .queryParam("pageSize", 20L)
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody(ReportExecutionResults::class.java)
      .value {
        assertThat(it!!.data).isNotEmpty()
        assertThat(it.policyBasedDataChanges).isEmpty()
      }
  }

  @Test
  fun `should return data inside results object for async dashboard data endpoint`() {
    val tableId = "tableId"
    val dpdId = "external-movements"
    val dashboardId = "dashboardId"
    val selectedPage = 2L
    val pageSize = 20L
    val data = listOf(
      mapOf(
        "establishment_id" to MetricData("KMI"),
        "has_ethnicity" to MetricData("10"),
        "ethnicity_is_missing" to MetricData("30"),
      ),
    )
    val expectedServiceResult = DashboardExecutionResults(
      data = data,
    )

    given(asyncDataApiService.getDashboardStatementResult(any(), any(), any(), any(), any(), any(), any()))
      .willReturn(data)

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/v2/reports/$dpdId/dashboards/$dashboardId/tables/$tableId/result")
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
      eq(selectedPage),
      eq(pageSize),
      eq(emptyMap()),
      any<ExecutionContext>(),
    )
  }

  private fun requestWithQueryAndAssert400(paramName: String, paramValue: Any, path: String) {
    requestWithQueryAndAssert400(mapOf(paramName to paramValue), path)
  }

  private fun requestWithQueryAndAssert400(params: Map<String, Any>, path: String) {
    requestWithQueryAndAssert(params, path).isBadRequest
  }

  private fun requestWithQueryAndAssert200(params: Map<String, Any>, path: String) {
    requestWithQueryAndAssert(params, path).isOk
  }

  private fun requestWithQueryAndAssert(params: Map<String, Any>, path: String): StatusAssertions = webTestClient.get()
    .uri { uriBuilder: UriBuilder ->
      params.entries.forEach { uriBuilder.queryParam(it.key, it.value) }

      uriBuilder.path(path).build()
    }
    .headers(setAuthorisation(roles = listOf(authorisedRole)))
    .exchange()
    .expectStatus()
}
