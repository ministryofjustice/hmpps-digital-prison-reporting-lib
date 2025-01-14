package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName

enum class Granularity {
  @SerializedName("hourly")
  HOURLY,

  @SerializedName("daily")
  DAILY,

  @SerializedName("weekly")
  WEEKLY,

  @SerializedName("monthly")
  MONTHLY,

  @SerializedName("quarterly")
  QUARTERLY,

  @SerializedName("annually")
  ANNUALLY,
}
