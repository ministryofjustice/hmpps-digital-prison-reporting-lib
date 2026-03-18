package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

data class DashboardVisualisationDefinition(
  val id: String,
  val type: DashboardVisualisationTypeDefinition,
  val display: String,
  val description: String? = null,
  val columns: DashboardVisualisationColumnsDefinition,
  val options: DashboardOptionDefinition? = null,
)
