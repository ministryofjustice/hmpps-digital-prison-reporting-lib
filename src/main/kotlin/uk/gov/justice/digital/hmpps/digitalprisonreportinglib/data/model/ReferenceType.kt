package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ReferenceType {
  @SerializedName("establishment")
  @SerialName("establishment")
  ESTABLISHMENT,

  @SerializedName("wing")
  @SerialName("wing")
  WING,

  @SerializedName("alert")
  @SerialName("alert")
  ALERT,
}
