package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Caseload

interface DprAuthAwareAuthenticationToken {
  fun getUsername(): String
  fun getActiveCaseLoadId(): String?
  fun getRoles(): List<String>
  fun getCaseLoadIds(): List<String>
  fun getCaseLoads(): List<Caseload>
}
