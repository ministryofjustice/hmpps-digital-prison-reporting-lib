package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class DashboardVisualisation(
  val id: String,
  val type: DashboardVisualisationType,
  val display: String? = null,
  val description: String? = null,
  val columns: DashboardVisualisationColumns,
  val showLatest: Boolean? = null,
  val columnsAsList: Boolean? = null,
)
