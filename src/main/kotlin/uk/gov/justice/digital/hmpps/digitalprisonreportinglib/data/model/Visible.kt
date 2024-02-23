package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName

enum class Visible {
  @SerializedName("true")
  TRUE,

  @SerializedName("false")
  FALSE,

  @SerializedName("mandatory")
  MANDATORY,
}
