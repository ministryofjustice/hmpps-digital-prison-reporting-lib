package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Datasource
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleDashboardProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.CaseloadResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.authentication.AuthUser
import uk.gov.justice.hmpps.kotlin.auth.AuthSource

data class ExecutionContext(
  val prisonCaseloadData: CaseloadResponse? = null,
  val userRoles: List<String>,
  val userInfo: AuthUser,
  val hasProbationDatasources: Boolean,
  val dataProductReportableInformation: DataProductReportableInformation,
  val preFilters: Map<String, String> = emptyMap(),
  val postFilters: Map<String, String> = emptyMap(),
  val sortColumnName: String? = null,
  val sortColumnDirection: String? = null,
  val columns: List<String> = emptyList(),
  val policies: List<Policy> = emptyList(),
) {
  constructor(
    username: String,
    reportId: String,
    reportVariantId: String,
    hasProbationDatasources: Boolean,
    preFilters: Map<String, String> = emptyMap(),
    postFilters: Map<String, String> = emptyMap(),
    sortColumnName: String? = null,
    sortColumnDirection: String? = null,
    columns: List<String> = emptyList(),
  ) : this(
    prisonCaseloadData = CaseloadResponse(
      username = username,
      active = true,
      accountType = "",
      activeCaseload = null,
      caseloads = emptyList(),
    ),
    userRoles = emptyList(),
    userInfo = AuthUser(
      username = username,
      active = true,
      name = "",
      authSource = AuthSource.NONE,
      userId = "",
      uuid = "",
    ),
    hasProbationDatasources = hasProbationDatasources,
    dataProductReportableInformation = DataProductReportableInformation(
      id = reportId,
      variantId = reportVariantId,
    ),
    preFilters = preFilters,
    postFilters = postFilters,
    sortColumnName = sortColumnName,
    sortColumnDirection = sortColumnDirection,
    columns = columns,
  )
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
 
  fun withReportDetails(
    reportDefinition: SingleReportProductDefinition,
  ): ExecutionContext = this.copy(
    policies = reportDefinition.policy,
    dataProductReportableInformation = dataProductReportableInformation.copy(
      id = reportDefinition.id,
      name = reportDefinition.name,
      variantName = reportDefinition.report.name,
      datasource = reportDefinition.datasource,
    ),
  )

  fun withDashboardDetails(
    dashboardDefinition: SingleDashboardProductDefinition,
  ): ExecutionContext = this.copy(
    policies = dashboardDefinition.policy,
    dataProductReportableInformation = this.dataProductReportableInformation.copy(
      id = dashboardDefinition.id,
      name = dashboardDefinition.name,
      variantName = dashboardDefinition.dashboard.name,
      datasource = dashboardDefinition.datasource,
    ),
  )
}

data class DataProductReportableInformation(
  val id: String,
  val name: String? = "",
  val datasource: Datasource? = null,
  val variantId: String,
  val variantName: String? = "",
)
