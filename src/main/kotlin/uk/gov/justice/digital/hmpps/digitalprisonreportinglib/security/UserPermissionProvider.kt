package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.authentication.AuthUser
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Caseload

interface UserPermissionProvider {
  fun getUserInfo(username: String): AuthUser
  fun getCaseloads(username: String): List<Caseload>
  fun getUsersRoles(username: String): List<String>
  fun getPrisonUsersCaseload(username: String): CaseloadResponse
}
