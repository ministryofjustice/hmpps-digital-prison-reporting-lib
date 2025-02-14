package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class Dashboard(
  val id: String,
  val name: String,
  val description: String,
  val dataset: String,
  val sections: List<DashboardSection>,
  val filter: ReportFilter? = null,
)
