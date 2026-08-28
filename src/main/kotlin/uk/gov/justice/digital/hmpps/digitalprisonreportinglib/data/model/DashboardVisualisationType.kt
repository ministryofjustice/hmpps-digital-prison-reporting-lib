package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName

enum class DashboardVisualisationType {
  @SerializedName("list")
  LIST,

  @SerializedName("doughnut")
  DOUGHNUT,

  @SerializedName("bar")
  BAR,

  @SerialName("bar-timeseries")
  @SerializedName("bar-timeseries")
  BAR_TIMESERIES,

  @SerializedName("line")
  LINE,

  @SerializedName("scorecard")
  SCORECARD,

  @SerialName("scorecard-group")
  @SerializedName("scorecard-group")
  SCORECARD_GROUP,

  @SerialName("matrix-timeseries")
  @SerializedName("matrix-timeseries")
  MATRIX_TIMESERIES,

  @SerialName("line-timeseries")
  @SerializedName("line-timeseries")
  LINE_TIMESERIES,
}
