package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class DashboardVisualisationColumn(
  val id: String,
  val display: String,
  val aggregate: AggregateType? = null,
  val unit: UnitType? = null,
  val displayValue: Boolean? = null,
  val axis: String? = null,
  val optional: Boolean? = null,
)
