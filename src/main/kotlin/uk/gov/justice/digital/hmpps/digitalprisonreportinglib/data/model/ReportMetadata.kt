package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportMetadata.Companion.INTERACTIVE_FLAG

@Serializable
data class ReportMetadata(
  val hints: List<ReportMetadataHint>,
) {
  companion object {
    const val INTERACTIVE_FLAG = "interactive"
  }
}

enum class ReportMetadataHint {
  @SerialName(INTERACTIVE_FLAG)
  @SerializedName(INTERACTIVE_FLAG)
  INTERACTIVE,
}
