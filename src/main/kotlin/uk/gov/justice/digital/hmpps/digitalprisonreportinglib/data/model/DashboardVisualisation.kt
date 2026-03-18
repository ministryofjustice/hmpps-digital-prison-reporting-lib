package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class DashboardVisualisation(
  val id: String,
  val type: DashboardVisualisationType,
  val display: String,
  val description: String? = null,
  val column: DashboardVisualisationColumns,
  val option: DashboardOption? = null,
)
