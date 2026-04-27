package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

interface DprAuthAwareAuthenticationToken {
  fun getUsername(): String
  fun getActiveCaseLoadId(): String?
  fun getRoles(): List<String>
  fun getCaseLoadIds(): List<String>
  fun getCaseLoads(): CaseloadResponse
}
