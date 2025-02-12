package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName

enum class AggregateType {
  @SerializedName("sum")
  SUM,

  @SerializedName("average")
  AVERAGE,
}
