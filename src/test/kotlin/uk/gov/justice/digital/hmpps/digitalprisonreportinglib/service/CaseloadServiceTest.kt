package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Caseload
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal

class CaseloadServiceTest {

  private val webClient = mock<WebClient>()
  private val caseloadService: CaseloadService = CaseloadService(webClient)

  @Test
  @SuppressWarnings("rawtypes")
  fun `get active caseload ID`() {
    val jwt = Jwt("token", Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS), mapOf("header1" to "value1"), mapOf("claim1" to "value1"))
    val headers = HttpHeaders()
    headers.set("Authorization", "Bearer $jwt")
    val expectedCaseloadResponse: CaseloadService.CaseloadResponse =
      CaseloadService.CaseloadResponse("user1", true, "GENERAL", Caseload("WWI", "WANDSWORTH (HMP)"), listOf(Caseload("WWI",  "WANDSWORTH (HMP)")))
    val requestHeadersUriSpec = mock<RequestHeadersUriSpec<*>>()
    whenever(webClient.get()).thenReturn(requestHeadersUriSpec)
    val requestHeaderSpec = mock<WebClient.RequestHeadersSpec<*>>()
    whenever(requestHeadersUriSpec.header(any(), anyVararg())).thenReturn(requestHeaderSpec)
    val responseSpec = mock<WebClient.ResponseSpec>()
    whenever(requestHeaderSpec.retrieve()).thenReturn(responseSpec)
    whenever(responseSpec.bodyToMono(CaseloadService.CaseloadResponse::class.java)).thenReturn(
      Mono.just(expectedCaseloadResponse))
    val actual = caseloadService.getActiveCaseloadId(jwt)

    assertEquals(expectedCaseloadResponse.activeCaseload.id, actual)
  }
}