package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class Metric(
  val id: String,
  val name: String,
  val display: String,
  val description: String,
  val charts: List<Chart>,
)

data class Chart(
  val type: ChartType,
  val label: Label,
  val unit: String,
  val columns: List<Column>,
)

data class Label(
  val name: String,
  val display: String,
)

data class Column(
  val name: String,
  val display: String,
)
