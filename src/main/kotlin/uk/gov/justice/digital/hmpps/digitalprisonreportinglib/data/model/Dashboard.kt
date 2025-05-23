package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class Dashboard(
  val id: String,
  val name: String,
  val description: String? = null,
  val dataset: String,
  val section: List<DashboardSection>,
  val filter: ReportFilter? = null,
)
