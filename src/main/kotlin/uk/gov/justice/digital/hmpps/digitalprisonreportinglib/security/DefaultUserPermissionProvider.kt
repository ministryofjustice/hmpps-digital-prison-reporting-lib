package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.NoDataAvailableException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.authentication.AuthUser
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Caseload
import uk.gov.justice.hmpps.kotlin.auth.AuthSource

const val WARNING_NO_ACTIVE_CASELOAD = "User has not set an active caseload."
const val WARNING_NO_CASELOADS = "User does not have any caseloads."

class DefaultUserPermissionProvider(private val manageUsersWebClient: WebClient) : UserPermissionProvider {
  override fun getCaseloads(username: String): List<Caseload> {
    val caseloadResponse = getPrisonUsersCaseload(username)

    if (caseloadResponse.caseloads.isEmpty() || caseloadResponse.activeCaseload == null) {
      val userInfo = getUserInfo(username)
      if (userInfo.authSource != AuthSource.NOMIS) {
        return emptyList()
      }
      if (caseloadResponse.caseloads.isEmpty()) {
        throw NoDataAvailableException(WARNING_NO_CASELOADS)
      }

      throw NoDataAvailableException(WARNING_NO_ACTIVE_CASELOAD)
    }

    return caseloadResponse.caseloads.sortedBy { it.id }.map { Caseload(it.id, it.name) }
  }

  override fun getUserInfo(username: String): AuthUser {
    val user = manageUsersWebClient.get()
      .uri("/users/$username/me")
      .header("Content-Type", "application/json")
      .retrieve()
      .bodyToMono(AuthUser::class.java)
      .block()!!

    if (user.authSource === AuthSource.NOMIS && user.activeCaseLoadId.isNullOrEmpty()) {
      throw NoDataAvailableException(WARNING_NO_ACTIVE_CASELOAD)
    }
    return user
  }

  override fun getUsersRoles(username: String): List<String> = manageUsersWebClient.get()
    .uri("/users/$username/roles")
    .header("Content-Type", "application/json")
    .retrieve()
    .bodyToMono(ROLES)
    .block()!!.map { it.roleCode }

  override fun getPrisonUsersCaseload(username: String): CaseloadResponse = manageUsersWebClient.get()
    .uri("/prisonusers/$username/caseloads")
    .header("Content-Type", "application/json")
    .exchangeToMono { response ->
      if (response.statusCode().value() == 404) {
        response.releaseBody().thenReturn(CaseloadResponse(
          username = username,
          active = false,
          accountType = AuthSource.DELIUS.toString(),
          caseloads = emptyList(),
          activeCaseload = null
        ))
      } else {
        response.bodyToMono(CaseloadResponse::class.java)
      }
    }
    .block()!!
}
data class RolesResponse(val roleCode: String)

private val ROLES: ParameterizedTypeReference<List<RolesResponse>> =
  object : ParameterizedTypeReference<List<RolesResponse>>() {}

data class CaseloadResponse(val username: String, val active: Boolean, val accountType: String, val activeCaseload: Caseload?, val caseloads: List<Caseload>)
