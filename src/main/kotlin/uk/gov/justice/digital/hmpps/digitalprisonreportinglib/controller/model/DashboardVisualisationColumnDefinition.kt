package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

data class DashboardVisualisationColumnDefinition(
  val id: String,
  val display: String,
  val aggregate: AggregateTypeDefinition? = null,
  val unit: UnitTypeDefinition? = null,
  val displayValue: Boolean? = null,
  val axis: String? = null,
  val optional: Boolean? = null,
)
