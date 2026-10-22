package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Datasource
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.CaseloadResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.authentication.AuthUser
import uk.gov.justice.hmpps.kotlin.auth.AuthSource

data class ExecutionContext(
  val prisonCaseloadData: CaseloadResponse? = null,
  val userRoles: List<String>,
  val userInfo: AuthUser,
  val hasProbationDatasources: Boolean,
  val dataProductReportableInformation: DataProductReportableInformation,
) {
  fun getActiveCaseLoadId(): String? = prisonCaseloadData?.activeCaseload?.id
  fun hasValidAuth(): Boolean {
    if (hasProbationDatasources) {
      return userInfo.username.isNotBlank() && userInfo.active && listOf(AuthSource.DELIUS.name.lowercase(), AuthSource.AUTH.name.lowercase()).contains(userInfo.authSource.name.lowercase())
    }

    if (userInfo.authSource == AuthSource.NOMIS && prisonCaseloadData != null) {
      return prisonCaseloadData.let {
        it.username.isNotBlank() && it.active && userInfo.username.isNotBlank() && userInfo.active
      }
    }
    return false
  }
  fun getCaseLoadIds(): List<String> = prisonCaseloadData?.caseloads?.map { it.id } ?: emptyList()
}

data class DataProductReportableInformation(
  val id: String,
  val name: String? = "",
  val datasource: Datasource? = null,
  val variantId: String,
  val variantName: String? = "",
)
