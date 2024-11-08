package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class Metric(
  val id: String,
  val name: String,
  val display: String,
  val description: String,
  val unit: String? = null,
  val charts: List<Chart>,
  val data: List<List<Data>>,
)

data class Chart(
  val type: ChartType,
  val dimension: String,
)

data class Data(
  val name: String,
  val display: String,
  val unit: String? = null,
)
