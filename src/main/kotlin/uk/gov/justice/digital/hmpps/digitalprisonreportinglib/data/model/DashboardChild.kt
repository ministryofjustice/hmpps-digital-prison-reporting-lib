package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DashboardChild(
  val dashboardId: String,
)
