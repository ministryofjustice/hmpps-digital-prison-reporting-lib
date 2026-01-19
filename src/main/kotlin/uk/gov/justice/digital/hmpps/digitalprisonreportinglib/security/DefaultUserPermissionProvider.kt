package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.NoDataAvailableException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Caseload

class DefaultUserPermissionProvider(private val manageUsersWebClient: WebClient) : UserPermissionProvider {

  override fun getActiveCaseloadId(username: String): String? {
    val caseloadResponse = getPrisonUsersCaseload(username)

    if (caseloadResponse.accountType != "GENERAL") {
      throw NoDataAvailableException("'${caseloadResponse.accountType}' account types are currently not supported.")
    }

    if (caseloadResponse.activeCaseload == null) {
      throw NoDataAvailableException(WARNING_NO_ACTIVE_CASELOAD)
    }

    return caseloadResponse.activeCaseload.id
  }

  override fun getCaseloads(username: String): List<Caseload> {
    val caseloadResponse = getPrisonUsersCaseload(username)

    if (caseloadResponse.caseloads.isEmpty()) {
      throw NoDataAvailableException(WARNING_NO_CASELOADS)
    }

    return caseloadResponse.caseloads.sortedBy { it.id }.map { Caseload(it.id, it.name) }
  }

  override fun getUsersRoles(username: String): List<String> = manageUsersWebClient.get()
    .uri("/users/$username/roles")
    .header("Content-Type", "application/json")
    .retrieve()
    .bodyToMono(ROLES)
    .block()!!.map { it.roleCode }

  private fun getPrisonUsersCaseload(username: String): CaseloadResponse = manageUsersWebClient.get()
    .uri("/prisonusers/$username/caseloads")
    .header("Content-Type", "application/json")
    .retrieve()
    .bodyToMono(CaseloadResponse::class.java)
    .block()!!
}

data class RolesResponse(val roleCode: String)

private val ROLES: ParameterizedTypeReference<List<RolesResponse>> =
  object : ParameterizedTypeReference<List<RolesResponse>>() {}

data class CaseloadResponse(val username: String, val active: Boolean, val accountType: String, val activeCaseload: Caseload?, val caseloads: List<Caseload>)
