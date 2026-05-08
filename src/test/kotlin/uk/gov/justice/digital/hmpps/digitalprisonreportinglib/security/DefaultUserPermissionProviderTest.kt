package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.NoDataAvailableException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Caseload
import uk.gov.justice.hmpps.kotlin.auth.AuthSource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DefaultUserPermissionProviderTest : IntegrationTestBase() {

  companion object {
    @JvmStatic
    @DynamicPropertySource
    fun registerProperties(registry: DynamicPropertyRegistry) {
      registry.add("dpr.lib.definition.locations") { "productDefinition.json" }
    }
  }

  private val webClient = mock<WebClient>()

  @Test
  fun `get available caseloads`() {
    val actual = manageUsersClient.getCaseloads("request-user")

    assertEquals(
      listOf(
        Caseload("WWI", "WANDSWORTH (HMP)"),
        Caseload("AKI", "Acklington (HMP)"),
        Caseload("LWSTMC", "Lowestoft (North East Suffolk) Magistrat"),
      ).sortedBy { it.id },
      actual.caseloads.sortedBy { it.id },
    )
  }

  @Test
  fun `getActiveCaseloadId should throw NoDataAvailableException for NOMIS account type with no active caseload`() {
    manageUsersMockServer.stubLookupUserCaseload("request-user", null)
    manageUsersMockServer.stubGetUserInfo("request-user", AuthSource.NOMIS)
    val exception = assertThrows<NoDataAvailableException> { manageUsersClient.getCaseloads("request-user") }

    assertEquals("User has not set an active caseload.", exception.reason)
  }

  @Test
  fun `getActiveCaseloadId should throw NoDataAvailableException for NOMIS account type with no caseloads`() {
    manageUsersMockServer.resetAll()
    manageUsersMockServer.stubLookupUsersRoles("request-user", listOf("INCIDENT_REPORTS__RO", "PRISONS_REPORTING_USER"))
    manageUsersMockServer.stubLookupUserCaseload("request-user", "WKI", "[]")
    manageUsersMockServer.stubGetUserInfo("request-user", AuthSource.NOMIS)
    val exception = assertThrows<NoDataAvailableException> { manageUsersClient.getCaseloads("request-user") }

    assertEquals("User does not have any caseloads.", exception.reason)
  }

  @Test
  fun `getActiveCaseloadId should not throw NoDataAvailableException for non-NOMIS account with no active caseload`() {
    manageUsersMockServer.stubLookupUserCaseload404("request-user")
    manageUsersMockServer.stubGetUserInfo(authSource = AuthSource.DELIUS)
    val info = manageUsersClient.getCaseloads("request-user")

    assertEquals(info.username, "request-user")
    assertEquals(info.activeCaseload, null)
  }

  private fun mockWebClientCall(expectedCaseloadResponse: CaseloadResponse) {
    val requestHeadersUriSpec = mock<RequestHeadersUriSpec<*>>()
    whenever(webClient.get()).thenReturn(requestHeadersUriSpec)
    whenever(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec)
    val requestHeaderSpec = mock<WebClient.RequestHeadersSpec<*>>()
    whenever(requestHeadersUriSpec.header(anyString(), anyVararg())).thenReturn(requestHeaderSpec)
    val responseSpec = mock<WebClient.ResponseSpec>()
    whenever(requestHeaderSpec.retrieve()).thenReturn(responseSpec)
    whenever(responseSpec.bodyToMono(CaseloadResponse::class.java)).thenReturn(
      Mono.just(expectedCaseloadResponse),
    )
  }
}
