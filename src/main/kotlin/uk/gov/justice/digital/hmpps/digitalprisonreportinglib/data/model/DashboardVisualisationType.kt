package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName

enum class DashboardVisualisationType {
  @SerializedName("list")
  LIST,

  @SerializedName("doughnut")
  DOUGHNUT,

  @SerializedName("bar")
  BAR,

  @SerializedName("bar-timeseries")
  BAR_TIMESERIES,

  @SerializedName("line")
  LINE,

  @SerializedName("scorecard")
  SCORECARD,

  @SerializedName("scorecard-group")
  SCORECARD_GROUP,

  @SerializedName("matrix-timeseries")
  MATRIX_TIMESERIES,

  @SerializedName("line-timeseries")
  LINE_TIMESERIES
}
