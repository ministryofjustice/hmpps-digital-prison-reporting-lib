package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.CaseloadResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.authentication.AuthUser
import uk.gov.justice.hmpps.kotlin.auth.AuthSource

data class ExecutionContext(
  val prisonCaseloadData: CaseloadResponse,
  val userRoles: List<String>,
  val userInfo: AuthUser,
) {
  fun getActiveCaseLoadId(): String? = prisonCaseloadData.activeCaseload?.id
  fun hasValidAuth(): Boolean = prisonCaseloadData.username.isNotBlank() && prisonCaseloadData.active && userInfo.username.isNotBlank() && userInfo.active && userInfo.authSource != AuthSource.NONE
  fun getCaseLoadIds(): List<String> = prisonCaseloadData.caseloads.map { it.id }
}
