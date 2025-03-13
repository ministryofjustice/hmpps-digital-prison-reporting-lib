package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Caseload
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken
import uk.gov.justice.hmpps.kotlin.auth.AuthSource

class DprUserAuthAwareAuthenticationToken(
  jwt: Jwt,
  clientId: String,
  userName: String? = null,
  authSource: AuthSource = AuthSource.NONE,
  authorities: Collection<GrantedAuthority> = emptyList(),
  private val caseloadProvider: CaseloadProvider,
) : AuthAwareAuthenticationToken(jwt, clientId, userName, authSource, authorities),
  DprAuthAwareAuthenticationToken {
  private val lock = Any()
  private var activeCaseload: String? = null
  private var caseloads: List<Caseload>? = null

  override fun getActiveCaseLoadId(): String? = synchronized(lock) {
    if (this.activeCaseload == null) {
      this.activeCaseload = caseloadProvider.getActiveCaseloadId(this.jwt)
    }

    return this.activeCaseload
  }

  override fun getUsername(): String = this.jwt.subject
  override fun getRoles(): List<String> = authorities?.mapNotNull { it.authority } ?: emptyList()

  override fun getCaseLoadIds(): List<String> = synchronized(lock) {
    if (this.caseloads == null) {
      this.caseloads = caseloadProvider.getCaseloads(this.jwt)
    }
    return this.caseloads!!.map { it.id }
  }

  override fun getCaseLoads(): List<Caseload> = synchronized(lock) {
    if (this.caseloads == null) {
      this.caseloads = caseloadProvider.getCaseloads(this.jwt)
    }
    return this.caseloads!!
  }
}
