package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class DashboardVisualisationColumns(
  val keys: List<DashboardVisualisationColumn>? = null,
  val measures: List<DashboardVisualisationColumn>,
  val filters: List<ValueVisualisationColumn>? = null,
  val expectNulls: Boolean,
)
