package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class Metric(
  val id: String,
  val dataset: String,
  val name: String,
  val display: String,
  val description: String,
  val visualisationType: List<DashboardChartType>,
  val specification: List<MetricSpecification>,
)

data class MetricSpecification(
  val name: String,
  val display: String,
  val unit: String? = null,
)
