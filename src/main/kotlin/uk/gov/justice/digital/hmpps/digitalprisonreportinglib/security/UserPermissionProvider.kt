package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Caseload

interface UserPermissionProvider {
  fun getActiveCaseloadId(username: String): String?

  fun getCaseloads(username: String): List<Caseload>

  fun getUsersRoles(username: String): List<String>
}
