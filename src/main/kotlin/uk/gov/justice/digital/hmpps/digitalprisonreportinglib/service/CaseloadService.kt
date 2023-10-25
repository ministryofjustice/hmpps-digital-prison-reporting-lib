package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Caseload

@Service
class CaseloadService(val webClient: WebClient) {

  fun getActiveCaseloadIds(jwt: Jwt): List<String> {
    val caseloadResponse = webClient.get().header("Authorization", "Bearer ${jwt.tokenValue}").retrieve().bodyToMono(CaseloadResponse::class.java).block()
    if (caseloadResponse.accountType != "GENERAL") {
      return emptyList()
    }
    return listOf(caseloadResponse.activeCaseload.id)
  }

  data class CaseloadResponse(val username: String, val active: Boolean, val accountType: String, val activeCaseload: Caseload, val caseloads: List<Caseload>)
}
