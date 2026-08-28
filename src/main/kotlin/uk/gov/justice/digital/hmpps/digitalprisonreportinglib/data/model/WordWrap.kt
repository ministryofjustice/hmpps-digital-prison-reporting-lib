package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName

enum class WordWrap {
  @SerializedName("none")
  None,

  @SerializedName("normal")
  Normal,

  @SerialName("break-words")
  @SerializedName("break-words")
  BreakWords,
}
