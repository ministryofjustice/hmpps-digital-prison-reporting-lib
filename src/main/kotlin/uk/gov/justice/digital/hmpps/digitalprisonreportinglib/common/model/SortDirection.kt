package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.common.model

import com.fasterxml.jackson.annotation.JsonValue
import com.google.gson.annotations.SerializedName

enum class SortDirection(@JsonValue val value: String) {
  @SerializedName("asc")
  ASC("asc"),

  @SerializedName("desc")
  DESC("desc"),
}