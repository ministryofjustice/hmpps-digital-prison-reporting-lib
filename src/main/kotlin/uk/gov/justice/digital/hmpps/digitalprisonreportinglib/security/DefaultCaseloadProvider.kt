package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.NoDataAvailableException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Caseload

const val WARNING_NO_ACTIVE_CASELOAD = "User has not set an active caseload."
const val WARNING_NO_CASELOADS = "User does not have any caseloads."

@Deprecated("Use UserPermissionProvider instead")
class DefaultCaseloadProvider(private val webClient: WebClient) : CaseloadProvider {

  override fun getActiveCaseloadId(jwt: Jwt): String {
    val caseloadResponse = getUsersCaseload(jwt)

    if (caseloadResponse.accountType != "GENERAL") {
      throw NoDataAvailableException("'${caseloadResponse.accountType}' account types are currently not supported.")
    }

    if (caseloadResponse.activeCaseload == null) {
      throw NoDataAvailableException(WARNING_NO_ACTIVE_CASELOAD)
    }

    return caseloadResponse.activeCaseload.id
  }

  override fun getCaseloads(jwt: Jwt): List<Caseload> {
    val caseloadResponse = getUsersCaseload(jwt)

    if (caseloadResponse.caseloads.isEmpty()) {
      throw NoDataAvailableException(WARNING_NO_CASELOADS)
    }

    return caseloadResponse.caseloads.sortedBy { it.id }.map { Caseload(it.id, it.name) }
  }

  private fun getUsersCaseload(jwt: Jwt): CaseloadResponse = webClient
    .get()
    .header("Authorization", "Bearer ${jwt.tokenValue}")
    .retrieve()
    .bodyToMono(CaseloadResponse::class.java)
    .block()!!
}
