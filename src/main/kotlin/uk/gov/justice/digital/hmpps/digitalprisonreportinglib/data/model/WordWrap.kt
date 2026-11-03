package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WordWrap {
  @SerialName("none")
  @SerializedName("none")
  None,

  @SerialName("normal")
  @SerializedName("normal")
  Normal,

  @SerialName("break-words")
  @SerializedName("break-words")
  BreakWords,
}
