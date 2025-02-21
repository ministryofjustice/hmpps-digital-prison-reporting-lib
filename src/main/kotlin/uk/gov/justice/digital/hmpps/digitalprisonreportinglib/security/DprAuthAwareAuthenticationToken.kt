package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Caseload

class DprAuthAwareAuthenticationToken(
  val jwt: Jwt,
  private val aPrincipal: String,
  authorities: Collection<GrantedAuthority>,
  private val caseloadProvider: CaseloadProvider,
) : JwtAuthenticationToken(jwt, authorities) {

  private val lock = Any()
  private var activeCaseload: String? = null
  private var caseloads: List<Caseload>? = null

  override fun getPrincipal(): String {
    return aPrincipal
  }

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
