package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.NoDataAvailableException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Caseload

const val WARNING_NO_ACTIVE_CASELOAD = "User has not set an active caseload."

class DefaultCaseloadProvider(private val webClient: WebClient) : CaseloadProvider {

  override fun getActiveCaseloadIds(jwt: Jwt): List<String> {
    val caseloadResponse = webClient
      .get()
      .header("Authorization", "Bearer ${jwt.tokenValue}")
      .retrieve()
      .bodyToMono(CaseloadResponse::class.java)
      .block()

    if (caseloadResponse!!.accountType != "GENERAL") {
      throw NoDataAvailableException("'${caseloadResponse.accountType}' account types are currently not supported.")
    }

    if (caseloadResponse.activeCaseload == null) {
      throw NoDataAvailableException(WARNING_NO_ACTIVE_CASELOAD)
    }

    return listOf(caseloadResponse.activeCaseload.id)
  }

  data class CaseloadResponse(val username: String, val active: Boolean, val accountType: String, val activeCaseload: Caseload?, val caseloads: List<Caseload>)
}
