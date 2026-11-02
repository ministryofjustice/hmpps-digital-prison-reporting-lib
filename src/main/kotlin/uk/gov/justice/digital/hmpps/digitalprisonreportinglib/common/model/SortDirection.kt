package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.common.model

import com.fasterxml.jackson.annotation.JsonValue
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SortDirection(@JsonValue val value: String) {
  @SerialName("asc")
  @SerializedName("asc")
  ASC("asc"),

  @SerialName("desc")
  @SerializedName("desc")
  DESC("desc"),
}
