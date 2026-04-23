package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import jakarta.persistence.EntityManager
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.context.annotation.Import
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.TestFlywayConfig
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.NoDataAvailableException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration.TestAuthenticationHelper
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration.wiremock.HmppsAuthMockServer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration.wiremock.ManageUsersMockServer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.productCollection.ProductCollectionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Caseload
import uk.gov.justice.hmpps.kotlin.auth.AuthSource
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DefaultUserPermissionProviderTest: IntegrationTestBase() {

  companion object {
    @JvmStatic
    @DynamicPropertySource
    fun registerProperties(registry: DynamicPropertyRegistry) {
      registry.add("dpr.lib.definition.locations") { "productDefinition.json" }
    }
  }

  private val webClient = mock<WebClient>()


  @Test
  fun `get active caseload ID`() {
    val actual = userPermissionProvider.getUserInfo("request-user").activeCaseLoadId

    assertEquals("LWSTMC", actual)
  }

  @Test
  fun `get available caseloads`() {
    val actual = userPermissionProvider.getCaseloads("request-user")

    assertEquals(listOf(
      Caseload("WWI", "WANDSWORTH (HMP)"),
      Caseload("AKI", "Acklington (HMP)"),
      Caseload("LWSTMC", "Lowestoft (North East Suffolk) Magistrat"),
    ).sortedBy { it.id }, actual.sortedBy { it.id })
  }

  @Test
  fun `getActiveCaseloadId should throw NoDataAvailableException for NOMIS account type with no active caseload`() {
    manageUsersMockServer.stubGetUserInfo("request-user", null)
    val exception = assertThrows<NoDataAvailableException> { userPermissionProvider.getUserInfo("request-user").activeCaseLoadId }

    assertEquals("User has not set an active caseload.", exception.reason)
  }

  @Test
  fun `getActiveCaseloadId should not throw NoDataAvailableException for non-NOMIS account with no active caseload`() {
    manageUsersMockServer.stubGetUserInfo("request-user", null, AuthSource.DELIUS)
    val info = userPermissionProvider.getUserInfo("request-user")

    assertEquals(info.username, "request-user")
    assertEquals(info.authSource, AuthSource.DELIUS)
    assertEquals(info.activeCaseLoadId, null)
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
