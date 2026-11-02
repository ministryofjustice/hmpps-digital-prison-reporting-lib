package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class DashboardVisualisationType {
  @SerialName("list")
  @SerializedName("list")
  LIST,

  @SerialName("doughnut")
  @SerializedName("doughnut")
  DOUGHNUT,

  @SerialName("bar")
  @SerializedName("bar")
  BAR,

  @SerialName("bar-timeseries")
  @SerializedName("bar-timeseries")
  BAR_TIMESERIES,

  @SerialName("line")
  @SerializedName("line")
  LINE,

  @SerialName("scorecard")
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
