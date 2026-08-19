package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Datasource
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.CaseloadResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.authentication.AuthUser
import uk.gov.justice.hmpps.kotlin.auth.AuthSource

data class ExecutionContext(
  val prisonCaseloadData: CaseloadResponse,
  val userRoles: List<String>,
  val userInfo: AuthUser,
  val hasProbationDatasources: Boolean,
  val dataProductReportableInformation: DataProductReportableInformation,
) {
  fun getActiveCaseLoadId(): String? = prisonCaseloadData.activeCaseload?.id
  fun hasValidAuth(): Boolean {
    if (hasProbationDatasources) {
      return userInfo.username.isNotBlank() && userInfo.active && listOf(AuthSource.DELIUS.name.lowercase(), AuthSource.AUTH.name.lowercase()).contains(userInfo.authSource.name.lowercase())
    }
    return prisonCaseloadData.username.isNotBlank() && prisonCaseloadData.active && userInfo.username.isNotBlank() && userInfo.active && userInfo.authSource == AuthSource.NOMIS
  }
  fun getCaseLoadIds(): List<String> = prisonCaseloadData.caseloads.map { it.id }
}

data class DataProductReportableInformation(
  val id: String,
  val name: String? = "",
  val datasource: Datasource? = null,
  val variantId: String,
  val variantName: String? = "",
)
