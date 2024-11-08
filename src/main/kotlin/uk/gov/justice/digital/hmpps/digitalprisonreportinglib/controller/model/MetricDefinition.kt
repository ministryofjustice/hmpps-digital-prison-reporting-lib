package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

data class MetricDefinition(
  val id: String,
  val name: String,
  val display: String,
  val description: String,
  val charts: List<ChartDefinition>,
  val data: List<List<DataDefinition>>,
)

data class ChartDefinition(
  val type: ChartTypeDefinition,
  val dimension: String,
)
data class DataDefinition(
  val name: String,
  val display: String,
  val unit: String? = null,
)
