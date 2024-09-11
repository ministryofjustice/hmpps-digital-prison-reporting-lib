package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

data class DashboardDefinition(
  val id: String,
  val name: String,
  val description: String,
  val metrics: List<DashboardMetricDefinition>,
) {
  data class DashboardMetricDefinition(
    val id: String,
    val visualisationType: List<DashboardChartTypeDefinition>,
  )
}
