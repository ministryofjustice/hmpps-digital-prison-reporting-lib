package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Granularity {
  @SerialName("hourly")
  @SerializedName("hourly")
  HOURLY,

  @SerialName("daily")
  @SerializedName("daily")
  DAILY,

  @SerialName("weekly")
  @SerializedName("weekly")
  WEEKLY,

  @SerialName("monthly")
  @SerializedName("monthly")
  MONTHLY,

  @SerialName("quarterly")
  @SerializedName("quarterly")
  QUARTERLY,

  @SerialName("annually")
  @SerializedName("annually")
  ANNUALLY,
}
