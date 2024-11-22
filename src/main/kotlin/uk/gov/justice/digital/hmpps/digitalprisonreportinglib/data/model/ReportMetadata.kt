package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportMetadata.Companion.INTERACTIVE_FLAG

data class ReportMetadata(
  val hints: List<ReportMetadataHint>,
) {
  companion object {
    const val INTERACTIVE_FLAG = "interactive"
  }
}

enum class ReportMetadataHint {
  @SerializedName(INTERACTIVE_FLAG)
  INTERACTIVE,
}
