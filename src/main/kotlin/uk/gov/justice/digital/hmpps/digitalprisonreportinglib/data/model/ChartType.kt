package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName

enum class ChartType() {
  @SerializedName("doughnut")
  DOUGHNUT,

  @SerializedName("bar")
  BAR,

  @SerializedName("line")
  LINE,
}
