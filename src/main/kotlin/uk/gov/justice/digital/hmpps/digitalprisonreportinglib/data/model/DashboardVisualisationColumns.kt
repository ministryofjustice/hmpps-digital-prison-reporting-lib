package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class DashboardVisualisationColumns(
  val key: List<DashboardVisualisationColumn>? = null,
  val measure: List<DashboardVisualisationColumn>,
  val filter: List<ValueVisualisationColumn>? = null,
  val expectNull: Boolean,
)
