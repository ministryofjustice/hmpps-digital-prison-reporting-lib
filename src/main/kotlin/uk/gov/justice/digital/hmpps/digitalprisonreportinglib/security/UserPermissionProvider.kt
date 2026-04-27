package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.authentication.AuthUser

interface UserPermissionProvider {
  fun getUserInfo(username: String): AuthUser
  fun getCaseloads(username: String): CaseloadResponse
  fun getUsersRoles(username: String): List<String>
}
