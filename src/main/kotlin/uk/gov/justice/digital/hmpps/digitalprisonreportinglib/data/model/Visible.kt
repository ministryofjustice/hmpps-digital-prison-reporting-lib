package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName

enum class Visible {
  @SerializedName(Companion.TRUE)
  @JsonProperty(Companion.TRUE)
  TRUE,

  @SerializedName(Companion.FALSE)
  @JsonProperty(Companion.FALSE)
  FALSE,

  @SerializedName(Companion.MANDATORY)
  @JsonProperty(Companion.MANDATORY)
  MANDATORY,

  ;

  companion object {
    private const val TRUE: String = "true"
    private const val FALSE: String = "false"
    private const val MANDATORY: String = "mandatory"
  }
}
