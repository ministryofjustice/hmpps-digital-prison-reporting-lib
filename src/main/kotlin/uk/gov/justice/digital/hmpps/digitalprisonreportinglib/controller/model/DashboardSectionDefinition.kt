package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

data class DashboardSectionDefinition(
  val id: String,
  val display: String,
  val description: String? = null,
  val visualisations: List<DashboardVisualisationDefinition>,
)
