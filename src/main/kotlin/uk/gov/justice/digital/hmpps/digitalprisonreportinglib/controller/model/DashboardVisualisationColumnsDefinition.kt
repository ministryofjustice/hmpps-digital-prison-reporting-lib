package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

data class DashboardVisualisationColumnsDefinition(
  val keys: List<DashboardVisualisationColumnDefinition>? = null,
  val measures: List<DashboardVisualisationColumnDefinition>,
  val filters: List<ValueVisualisationColumnDefinition>? = null,
  val expectNulls: Boolean,
)
