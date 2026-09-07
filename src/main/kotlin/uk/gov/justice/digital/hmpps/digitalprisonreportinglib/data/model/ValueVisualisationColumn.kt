package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ValueVisualisationColumn(
  val id: String,
  val equals: String?,
)
