package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.NoDataAvailableException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Caseload

class DefaultUserPermissionProviderTest {

  private val webClient = mock<WebClient>()
  private val userPermissionProvider: UserPermissionProvider = DefaultUserPermissionProvider(webClient)

  @Test
  fun `get active caseload ID`() {
    val expectedCaseloadResponse =
      CaseloadResponse(
        "user1",
        true,
        "GENERAL",
        Caseload("WWI", "WANDSWORTH (HMP)"),
        listOf(
          Caseload("WWI", "WANDSWORTH (HMP)"),
        ),
      )
    mockWebClientCall(expectedCaseloadResponse)
    val actual = userPermissionProvider.getActiveCaseloadId("user1")

    assertEquals(expectedCaseloadResponse.activeCaseload!!.id, actual)
  }

  @Test
  fun `get available caseloads`() {
    val expectedCaseloadResponse =
      CaseloadResponse("user1", true, "GENERAL", Caseload("WWI", "WANDSWORTH (HMP)"), listOf(Caseload("WWI", "WANDSWORTH (HMP)"), Caseload("LEI", "Leeds (HMP)")))
    mockWebClientCall(expectedCaseloadResponse)
    val actual = userPermissionProvider.getCaseloads("user1")

    assertEquals(expectedCaseloadResponse.caseloads.sortedBy { it.id }, actual)
  }

  @Test
  fun `getActiveCaseloadId should throw NoDataAvailableException for any account type other than GENERAL`() {
    val expectedCaseloadResponse =
      CaseloadResponse("user1", true, "GLOBAL_SEARCH", Caseload("WWI", "WANDSWORTH (HMP)"), listOf(Caseload("WWI", "WANDSWORTH (HMP)")))
    mockWebClientCall(expectedCaseloadResponse)
    val exception = assertThrows<NoDataAvailableException> { userPermissionProvider.getActiveCaseloadId("user1") }

    assertEquals(exception.reason, "'GLOBAL_SEARCH' account types are currently not supported.")
  }

  @Test
  fun `getActiveCaseloadId should throw an exception in no caseload is active`() {
    val expectedCaseloadResponse =
      CaseloadResponse("user1", true, "GENERAL", null, listOf(Caseload("WWI", "WANDSWORTH (HMP)")))
    mockWebClientCall(expectedCaseloadResponse)
    val exception = assertThrows<NoDataAvailableException> { userPermissionProvider.getActiveCaseloadId("user1") }

    assertEquals(exception.reason, "User has not set an active caseload.")
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
