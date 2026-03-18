package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class DashboardSection(
  val id: String,
  val display: String,
  val description: String? = null,
  val visualisation: List<DashboardVisualisation>,
)
