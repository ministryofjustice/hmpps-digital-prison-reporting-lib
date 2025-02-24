package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Caseload
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken
import uk.gov.justice.hmpps.kotlin.auth.AuthSource

class DprAuthAwareAuthenticationToken(
  jwt: Jwt,
  clientId: String,
  userName: String? = null,
  authSource: AuthSource = AuthSource.NONE,
  authorities: Collection<GrantedAuthority> = emptyList(),
  private val caseloadProvider: CaseloadProvider,
) : AuthAwareAuthenticationToken(jwt, clientId, userName, authSource, authorities) {
  private val lock = Any()
  private var activeCaseload: String? = null
  private var caseloads: List<Caseload>? = null

  fun getActiveCaseLoadId(): String? = synchronized(lock) {
    if (this.activeCaseload == null) {
      this.activeCaseload = caseloadProvider.getActiveCaseloadId(this.jwt)
    }

    return this.activeCaseload
  }

  fun getCaseLoadIds(): List<String> = synchronized(lock) {
    if (this.caseloads == null) {
      this.caseloads = caseloadProvider.getCaseloads(this.jwt)
    }
    return this.caseloads!!.map { it.id }
  }

  fun getCaseLoads(): List<Caseload> = synchronized(lock) {
    if (this.caseloads == null) {
      this.caseloads = caseloadProvider.getCaseloads(this.jwt)
    }
    return this.caseloads!!
  }
}
