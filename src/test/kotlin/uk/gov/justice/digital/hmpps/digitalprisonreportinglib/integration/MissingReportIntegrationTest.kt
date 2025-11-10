package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.container.PostgresContainer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration.IntegrationSystemTestBase.Companion.manageUsersMockServer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.missingReport.MissingReportSubmission

class MissingReportNoDatasourceIntegrationTest : IntegrationTestBase() {

  companion object {
    @JvmStatic
    @DynamicPropertySource
    fun registerProperties(registry: DynamicPropertyRegistry) {
      registry.add("dpr.lib.definition.locations") { "productDefinition.json" }
    }
  }

  @Test
  fun `using the missing report endpoint does not work`() {
    webTestClient.post()
      .uri("/missingRequest/external-movements/last-month")
      .bodyValue("a reason")
      .headers(setAuthorisation(roles = listOf(authorisedRole), user = "foo"))
      .exchange()
      .expectStatus().isEqualTo(404)
  }
}

class MissingReportIntegrationTest : IntegrationTestBase() {
  companion object {

    val pgContainer = PostgresContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun setupClass(registry: DynamicPropertyRegistry) {
      pgContainer?.run {
        registry.add("spring.datasource.url", pgContainer::getJdbcUrl)
        registry.add("spring.datasource.username", pgContainer::getUsername)
        registry.add("spring.datasource.password", pgContainer::getPassword)
        registry.add("spring.datasource.missingreport.url", pgContainer::getJdbcUrl)
        registry.add("spring.datasource.missingreport.username", pgContainer::getUsername)
        registry.add("spring.datasource.missingreport.password", pgContainer::getPassword)
      }
    }

    @JvmStatic
    @DynamicPropertySource
    fun registerProperties(registry: DynamicPropertyRegistry) {
      registry.add("dpr.lib.aws.dynamodb.enabled") { "true" }
      registry.add("dpr.lib.aws.accountId") { "1" }
      registry.add("dpr.lib.definition.locations") { "productDefinition.json" }
    }
  }

  @BeforeEach
  fun setUp() {
    manageUsersMockServer.stubLookupUsersRoles("request-user", listOf("INCIDENT_REPORTS__RO"))
    manageUsersMockServer.stubLookupUserCaseload()
  }

  @Test
  fun `posting a missing report submission works as intended`() {
    val result = webTestClient.post()
      .uri("/missingRequest/external-movements/last-month")
      .bodyValue("a reason")
      .headers(setAuthorisation(roles = listOf(authorisedRole), user = "foo"))
      .exchange()
      .expectStatus().isOk
      .expectBody<MissingReportSubmission>()
      .returnResult()
    assertThat(result.responseBody).isNotNull()
    assertThat(result.responseBody!!.userId).isEqualTo("foo")
    assertThat(result.responseBody!!.reportId).isEqualTo("external-movements")
    assertThat(result.responseBody!!.reportVariantId).isEqualTo("last-month")
    assertThat(result.responseBody!!.reason).isEqualTo("a reason")
    assertThat(result.responseBody!!.id).isGreaterThan(0)
  }
}
