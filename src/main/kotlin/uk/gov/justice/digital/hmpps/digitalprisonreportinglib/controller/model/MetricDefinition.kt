package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

data class MetricDefinition(
  val id: String,
  val name: String,
  val display: String,
  val description: String,
  val visualisationType: List<DashboardChartTypeDefinition>,
  val specification: List<MetricSpecificationDefinition>,
)

data class MetricSpecificationDefinition(
  val name: String,
  val display: String,
  val unit: String? = null,
)
