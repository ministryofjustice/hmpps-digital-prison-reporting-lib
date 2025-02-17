package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

import com.google.gson.annotations.SerializedName

enum class UnitTypeDefinition {
  @SerializedName("number")
  NUMBER,

  @SerializedName("percentage")
  PERCENTAGE,
}
