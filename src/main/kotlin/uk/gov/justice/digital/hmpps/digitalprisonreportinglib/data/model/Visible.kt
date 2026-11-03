package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Visible {

  @SerialName("true")
  @SerializedName("true")
  TRUE,

  @SerialName("false")
  @SerializedName("false")
  FALSE,

  @SerialName("mandatory")
  @SerializedName("mandatory")
  MANDATORY,
}
