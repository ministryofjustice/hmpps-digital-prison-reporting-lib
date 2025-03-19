package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

data class DashboardVisualisationColumnsDefinition(
  val key: List<DashboardVisualisationColumnDefinition>? = null,
  val measure: List<DashboardVisualisationColumnDefinition>,
  val filters: List<ValueVisualisationColumnDefinition>? = null,
  val expectNull: Boolean,
)
