package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DashboardBucket(
  val min: Long? = null,
  val max: Long? = null,
  val hexColour: String? = null,
)
