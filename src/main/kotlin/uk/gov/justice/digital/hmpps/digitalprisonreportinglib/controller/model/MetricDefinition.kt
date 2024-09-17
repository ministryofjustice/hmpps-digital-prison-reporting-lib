package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

data class MetricDefinition(
  val id: String,
  val name: String,
  val display: String,
  val description: String,
  val specification: List<MetricSpecificationDefinition>,
)

data class MetricSpecificationDefinition(
  val name: String,
  val display: String,
  val unit: String? = null,
  val chart: List<DashboardChartTypeDefinition>? = null,
  val group: Boolean? = null,
)
