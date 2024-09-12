package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class Dashboard(
  val id: String,
  val name: String,
  val description: String,
  val metrics: List<DashboardMetric>,
) {
  data class DashboardMetric(
    val id: String,
    val visualisationType: List<DashboardChartType>,
  )
}
