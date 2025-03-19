package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

data class DashboardSectionDefinition(
  val id: String,
  val display: String? = null,
  val description: String? = null,
  val visualisation: List<DashboardVisualisationDefinition>,
)
