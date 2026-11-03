package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AggregateType {
  @SerializedName("sum")
  @SerialName("sum")
  SUM,

  @SerializedName("average")
  @SerialName("average")
  AVERAGE,
}
