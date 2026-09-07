package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PolicyType(val type: String) {
  @SerialName("row-level")
  ROW_LEVEL("row-level"),
  ACCESS("access"),
  LAO("lao"),
}
