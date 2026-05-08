package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken
import uk.gov.justice.hmpps.kotlin.auth.AuthSource

class DprSystemAuthAwareAuthenticationToken(
  jwt: Jwt,
  clientId: String,
  userName: String? = null,
  authSource: AuthSource = AuthSource.NONE,
  authorities: Collection<GrantedAuthority> = emptyList(),
  private val manageUsersClient: ManageUsersClient,
) : AuthAwareAuthenticationToken(jwt, clientId, userName, authSource, authorities),
  DprAuthAwareAuthenticationToken {
  private var activeCaseload: String? = null
  private var caseloads: CaseloadResponse? = null
  private var roles: List<String>? = null

  override fun getUsername(): String = userName!!

  override fun getActiveCaseLoadId(): String? {
    if (this.activeCaseload == null) {
      this.activeCaseload = this.userName?.let {
        getCaseLoads().activeCaseload?.id
      }
    }
    return this.activeCaseload
  }

  override fun getCaseLoadIds(): List<String> {
    if (this.caseloads == null) {
      this.caseloads = getCaseLoads()
    }
    return this.caseloads!!.caseloads.map { it.id }
  }

  override fun getCaseLoads(): CaseloadResponse {
    if (this.caseloads == null) {
      this.caseloads = this.userName?.let {
        manageUsersClient.getCaseloads(it)
      } ?: CaseloadResponse(
        username = "",
        active = false,
        accountType = "GENERAL",
        activeCaseload = null,
        caseloads = emptyList(),
      )
    }
    return this.caseloads!!
  }

  override fun getRoles(): List<String> {
    if (this.roles == null) {
      this.roles = this.userName?.let {
        manageUsersClient.getUsersRoles(it)
      } ?: emptyList()
    }
    return this.roles!!
  }
}
