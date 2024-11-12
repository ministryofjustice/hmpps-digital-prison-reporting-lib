package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

data class MetricDefinition(
  val id: String,
  val name: String,
  val display: String,
  val description: String,
  val charts: List<ChartDefinition>,
)

data class ChartDefinition(
  val type: ChartTypeDefinition,
  val label: LabelDefinition,
  val unit: String,
  val columns: List<ColumnDefinition>,
)

data class LabelDefinition(
  val name: String,
  val display: String,
)
data class ColumnDefinition(
  val name: String,
  val display: String,
)
