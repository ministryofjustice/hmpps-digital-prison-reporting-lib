package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration

import com.google.gson.Gson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.container.PostgresContainer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.ExecutionContext
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardExecutionResults
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.MetricData
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ReportExecutionResults
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ResultType
import uk.gov.justice.hmpps.kotlin.auth.AuthSource
import java.sql.DriverManager

class V2DataApiIntegrationTest : IntegrationTestBase() {
  companion object {
    @JvmStatic
    @DynamicPropertySource
    fun registerProperties(registry: DynamicPropertyRegistry) {
      registry.add("dpr.lib.definition.locations") { "productDefinitionWithLaoPolicy.json" }
      registry.add("dpr.lib.hasProbationDatasources") { true }
      registry.add("dpr.lib.user.requiredAuthSources") { "DELIUS,AUTH" }
    }
  }

  @BeforeEach
  fun laoSetup() {
    super.setup()
    DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
      .prepareStatement("TRUNCATE TABLE product_.lao_exclusions; TRUNCATE TABLE product_.lao_restrictions; TRUNCATE TABLE product_.lao_crns;").execute()
    manageUsersMockServer.stubGetUserInfo(username = "P111111", authSource = AuthSource.DELIUS)
    manageUsersMockServer.stubLookupUsersRoles("P111111", listOf(authorisedRole))
    stubDefinitionsResponse()
  }

  @AfterEach
  fun tearDown() {
    DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
      .prepareStatement("TRUNCATE TABLE product_.lao_exclusions; TRUNCATE TABLE product_.lao_restrictions; TRUNCATE TABLE product_.lao_crns;").execute()
  }

  @Test
  fun `should return data inside results object for sync report data endpoint`() {
    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/v2/reports/external-movements/last-month")
          .build()
      }
      .headers(setAuthorisation(user = "P111111", roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody(ReportExecutionResults::class.java)
      .value {
        assertThat(it!!.data).isNotEmpty()
        assertThat(it.policyBasedDataChanges).isEmpty()
        assertThat(it.resultType).isEqualTo(ResultType.REPORT)
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
      .headers(setAuthorisation(user = "P111111", roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody(DashboardExecutionResults::class.java)
      .value {
        assertThat(it!!.data).isNotEmpty()
        assertThat(it.policyBasedDataChanges).isEmpty()
        assertThat(it.resultType).isEqualTo(ResultType.DASHBOARD)
      }
  }

  @Test
  fun `should return data inside results object for async report data endpoint`() {
    val tableId = "tableId"
    val selectedPage = 2L
    val pageSize = 20L
    val expectedServiceResult = ReportExecutionResults(
      emptyMap(),
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
      .headers(setAuthorisation(user = "P111111", roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody(ReportExecutionResults::class.java)
      .value {
        assertThat(it!!.data).isEqualTo(expectedServiceResult.data)
        assertThat(it.policyBasedDataChanges).isEmpty()
        assertThat(it.resultType).isEqualTo(ResultType.REPORT)
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
      .willReturn(DashboardExecutionResults(emptyMap(), data))

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/v2/reports/$dpdId/dashboards/$dashboardId/tables/$tableId/result")
          .queryParam("selectedPage", selectedPage)
          .queryParam("pageSize", pageSize)
          .build()
      }
      .headers(setAuthorisation(user = "P111111", roles = listOf(authorisedRole)))
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
}
