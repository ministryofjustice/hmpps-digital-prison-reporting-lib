package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Effect(val type: String) {
  @SerialName("permit")
  PERMIT("permit"),

  @SerialName("deny")
  DENY("deny"),
}
