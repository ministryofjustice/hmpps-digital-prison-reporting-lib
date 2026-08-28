package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DashboardVisualisationColumn(
  val id: String,
  val display: String? = null,
  val aggregate: AggregateType? = null,
  val unit: UnitType? = null,
  val displayValue: Boolean? = null,
  val axis: String? = null,
  val optional: Boolean? = null,
)
