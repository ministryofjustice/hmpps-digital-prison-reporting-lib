package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName

enum class ReferenceType {
  @SerializedName("establishment")
  ESTABLISHMENT,

  @SerializedName("wing")
  WING,

  @SerializedName("alert")
  ALERT,
}
