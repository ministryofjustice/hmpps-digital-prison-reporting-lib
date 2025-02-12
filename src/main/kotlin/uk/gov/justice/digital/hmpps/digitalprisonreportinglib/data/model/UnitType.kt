package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName

enum class UnitType {
  @SerializedName("number")
  NUMBER,

  @SerializedName("percentage")
  PERCENTAGE,
}
