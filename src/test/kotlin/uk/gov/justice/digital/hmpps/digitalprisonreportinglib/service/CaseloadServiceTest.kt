package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.http.HttpHeaders
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Caseload
import java.time.Instant
import java.time.temporal.ChronoUnit

class CaseloadServiceTest {

  private val webClient = mock<WebClient>()
  private val caseloadService: CaseloadService = CaseloadService(webClient)

  @Test
  @SuppressWarnings("rawtypes")
  fun `get active caseload ID`() {
    val jwt = createJwtHeaders()
    val expectedCaseloadResponse: CaseloadService.CaseloadResponse =
      CaseloadService.CaseloadResponse("user1", true, "GENERAL", Caseload("WWI", "WANDSWORTH (HMP)"), listOf(Caseload("WWI",  "WANDSWORTH (HMP)")))
    mockWebClientCall(expectedCaseloadResponse)
    val actual = caseloadService.getActiveCaseloadIds(jwt)

    assertEquals(listOf(expectedCaseloadResponse.activeCaseload.id), actual)
  }
  @Test
  fun `getActiveCaseloadId should return an empty list for any account type other than GENERAL`() {
    val jwt = createJwtHeaders()
    val expectedCaseloadResponse: CaseloadService.CaseloadResponse =
      CaseloadService.CaseloadResponse("user1", true, "GLOBAL_SEARCH", Caseload("WWI", "WANDSWORTH (HMP)"), listOf(Caseload("WWI",  "WANDSWORTH (HMP)")))
    mockWebClientCall(expectedCaseloadResponse)
    val actual = caseloadService.getActiveCaseloadIds(jwt)

    assertEquals(emptyList<String>(), actual)
  }

  private fun createJwtHeaders(): Jwt {
    val jwt = Jwt("token", Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS), mapOf("header1" to "value1"), mapOf("claim1" to "value1"))
    val headers = HttpHeaders()
    headers.set("Authorization", "Bearer $jwt")
    return jwt
  }

  private fun mockWebClientCall(expectedCaseloadResponse: CaseloadService.CaseloadResponse) {
    val requestHeadersUriSpec = mock<RequestHeadersUriSpec<*>>()
    whenever(webClient.get()).thenReturn(requestHeadersUriSpec)
    val requestHeaderSpec = mock<WebClient.RequestHeadersSpec<*>>()
    whenever(requestHeadersUriSpec.header(any(), anyVararg())).thenReturn(requestHeaderSpec)
    val responseSpec = mock<WebClient.ResponseSpec>()
    whenever(requestHeaderSpec.retrieve()).thenReturn(responseSpec)
    whenever(responseSpec.bodyToMono(CaseloadService.CaseloadResponse::class.java)).thenReturn(
      Mono.just(expectedCaseloadResponse),
    )
  }
}