package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class UnitType {
  @SerializedName("number")
  @SerialName("number")
  NUMBER,

  @SerializedName("percentage")
  @SerialName("percentage")
  PERCENTAGE,
}
