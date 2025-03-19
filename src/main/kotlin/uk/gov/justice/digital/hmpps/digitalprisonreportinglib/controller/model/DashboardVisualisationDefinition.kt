package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

data class DashboardVisualisationDefinition(
  val id: String,
  val type: DashboardVisualisationTypeDefinition,
  val display: String? = null,
  val description: String? = null,
  val column: DashboardVisualisationColumnsDefinition,
)
