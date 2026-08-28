package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName

enum class RenderMethod {
  HTML,

  @SerialName("HTML-child")
  @SerializedName("HTML-child")
  HTMLChild,
  PDF,
  SVG,
}
