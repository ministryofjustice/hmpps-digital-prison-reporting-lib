package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName

enum class WordWrap {
  @SerializedName("none")
  None,

  @SerializedName("normal")
  Normal,

  @SerializedName("break-words")
  BreakWords,
}
