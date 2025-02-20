package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpHeaders
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.NoDataAvailableException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Caseload
import java.time.Instant
import java.time.temporal.ChronoUnit

class DefaultCaseloadProviderTest {

  private val webClient = mock<WebClient>()
  private val caseloadProvider: CaseloadProvider = DefaultCaseloadProvider(webClient)

  @Test
  @SuppressWarnings("rawtypes")
  fun `get active caseload ID`() {
    val jwt = createJwtHeaders()
    val expectedCaseloadResponse: DefaultCaseloadProvider.CaseloadResponse =
      DefaultCaseloadProvider.CaseloadResponse("user1", true, "GENERAL", Caseload("WWI", "WANDSWORTH (HMP)"), listOf(Caseload("WWI", "WANDSWORTH (HMP)")))
    mockWebClientCall(expectedCaseloadResponse)
    val actual = caseloadProvider.getActiveCaseloadId(jwt)

    assertEquals(expectedCaseloadResponse.activeCaseload!!.id, actual)
  }

  @Test
  @SuppressWarnings("rawtypes")
  fun `get available caseloads`() {
    val jwt = createJwtHeaders()
    val expectedCaseloadResponse: DefaultCaseloadProvider.CaseloadResponse =
      DefaultCaseloadProvider.CaseloadResponse("user1", true, "GENERAL", Caseload("WWI", "WANDSWORTH (HMP)"), listOf(Caseload("WWI", "WANDSWORTH (HMP)"), Caseload("LEI", "Leeds (HMP)")))
    mockWebClientCall(expectedCaseloadResponse)
    val actual = caseloadProvider.getCaseloads(jwt)

    assertEquals(expectedCaseloadResponse.caseloads.sortedBy { it.id }, actual)
  }

  @Test
  fun `getActiveCaseloadId should throw NoDataAvailableException for any account type other than GENERAL`() {
    val jwt = createJwtHeaders()
    val expectedCaseloadResponse: DefaultCaseloadProvider.CaseloadResponse =
      DefaultCaseloadProvider.CaseloadResponse("user1", true, "GLOBAL_SEARCH", Caseload("WWI", "WANDSWORTH (HMP)"), listOf(Caseload("WWI", "WANDSWORTH (HMP)")))
    mockWebClientCall(expectedCaseloadResponse)
    val exception = assertThrows<NoDataAvailableException> { caseloadProvider.getActiveCaseloadId(jwt) }

    assertEquals(exception.reason, "'GLOBAL_SEARCH' account types are currently not supported.")
  }

  @Test
  fun `getActiveCaseloadId should throw an exception in no caseload is active`() {
    val jwt = createJwtHeaders()
    val expectedCaseloadResponse: DefaultCaseloadProvider.CaseloadResponse =
      DefaultCaseloadProvider.CaseloadResponse("user1", true, "GENERAL", null, listOf(Caseload("WWI", "WANDSWORTH (HMP)")))
    mockWebClientCall(expectedCaseloadResponse)
    val exception = assertThrows<NoDataAvailableException> { caseloadProvider.getActiveCaseloadId(jwt) }

    assertEquals(exception.reason, "User has not set an active caseload.")
  }

  private fun createJwtHeaders(): Jwt {
    val jwt = Jwt("token", Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS), mapOf("header1" to "value1"), mapOf("claim1" to "value1"))
    val headers = HttpHeaders()
    headers.set("Authorization", "Bearer $jwt")
    return jwt
  }

  private fun mockWebClientCall(expectedCaseloadResponse: DefaultCaseloadProvider.CaseloadResponse) {
    val requestHeadersUriSpec = mock<RequestHeadersUriSpec<*>>()
    whenever(webClient.get()).thenReturn(requestHeadersUriSpec)
    val requestHeaderSpec = mock<WebClient.RequestHeadersSpec<*>>()
    whenever(requestHeadersUriSpec.header(any(), anyVararg())).thenReturn(requestHeaderSpec)
    val responseSpec = mock<WebClient.ResponseSpec>()
    whenever(requestHeaderSpec.retrieve()).thenReturn(responseSpec)
    whenever(responseSpec.bodyToMono(DefaultCaseloadProvider.CaseloadResponse::class.java)).thenReturn(
      Mono.just(expectedCaseloadResponse),
    )
  }
}
