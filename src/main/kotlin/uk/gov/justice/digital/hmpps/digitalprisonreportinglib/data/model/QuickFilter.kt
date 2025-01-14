package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName

enum class QuickFilter {
  @SerializedName("today")
  TODAY,

  @SerializedName("yesterday")
  YESTERDAY,

  @SerializedName("last-seven-days")
  LAST_SEVEN_DAYS,

  @SerializedName("last-thirty-days")
  LAST_THIRTY_DAYS,

  @SerializedName("last-month")
  LAST_MONTH,

  @SerializedName("last-full-month")
  LAST_FULL_MONTH,

  @SerializedName("last-90-days")
  LAST_90_DAYS,

  @SerializedName("last-full-3-months")
  LAST_FULL_3_MONTHS,

  @SerializedName("last-year")
  LAST_YEAR,

  @SerializedName("last-full-year")
  LAST_FULL_YEAR,

  @SerializedName("tomorrow")
  TOMORROW,

  @SerializedName("next-seven-days")
  NEXT_SEVEN_DAYS,

  @SerializedName("next-thirty-days")
  NEXT_THIRTY_DAYS,

  @SerializedName("next-month")
  NEXT_MONTH,

  @SerializedName("next-full-month")
  NEXT_FULL_MONTH,

  @SerializedName("next-90-days")
  NEXT_90_DAYS,

  @SerializedName("next-3-months")
  NEXT_3_MONTHS,

  @SerializedName("next-full-3-months")
  NEXT_FULL_3_MONTHS,

  @SerializedName("next-year")
  NEXT_YEAR,

  @SerializedName("next-full-year")
  NEXT_FULL_YEAR,
}
