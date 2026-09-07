package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName

enum class QuickFilter {
  @SerialName("today")
  @SerializedName("today")
  TODAY,

  @SerialName("yesterday")
  @SerializedName("yesterday")
  YESTERDAY,

  @SerialName("last-seven-days")
  @SerializedName("last-seven-days")
  LAST_SEVEN_DAYS,

  @SerialName("last-thirty-days")
  @SerializedName("last-thirty-days")
  LAST_THIRTY_DAYS,

  @SerialName("last-month")
  @SerializedName("last-month")
  LAST_MONTH,

  @SerialName("last-full-month")
  @SerializedName("last-full-month")
  LAST_FULL_MONTH,

  @SerialName("last-ninety-days")
  @SerializedName("last-ninety-days")
  LAST_90_DAYS,

  @SerialName("last-three-months")
  @SerializedName("last-three-months")
  LAST_THREE_MONTHS,

  @SerialName("last-full-three-months")
  @SerializedName("last-full-three-months")
  LAST_FULL_3_MONTHS,

  @SerialName("last-year")
  @SerializedName("last-year")
  LAST_YEAR,

  @SerialName("last-full-year")
  @SerializedName("last-full-year")
  LAST_FULL_YEAR,

  @SerialName("tomorrow")
  @SerializedName("tomorrow")
  TOMORROW,

  @SerialName("next-seven-days")
  @SerializedName("next-seven-days")
  NEXT_SEVEN_DAYS,

  @SerialName("next-thirty-days")
  @SerializedName("next-thirty-days")
  NEXT_THIRTY_DAYS,

  @SerialName("next-month")
  @SerializedName("next-month")
  NEXT_MONTH,

  @SerialName("next-full-month")
  @SerializedName("next-full-month")
  NEXT_FULL_MONTH,

  @SerialName("next-ninety-days")
  @SerializedName("next-ninety-days")
  NEXT_90_DAYS,

  @SerialName("next-three-months")
  @SerializedName("next-three-months")
  NEXT_3_MONTHS,

  @SerialName("next-full-three-months")
  @SerializedName("next-full-three-months")
  NEXT_FULL_3_MONTHS,

  @SerialName("next-year")
  @SerializedName("next-year")
  NEXT_YEAR,

  @SerialName("next-full-year")
  @SerializedName("next-full-year")
  NEXT_FULL_YEAR,
}
