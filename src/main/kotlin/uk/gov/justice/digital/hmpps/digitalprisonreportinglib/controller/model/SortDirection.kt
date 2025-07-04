package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

import com.fasterxml.jackson.annotation.JsonValue
import com.google.gson.annotations.SerializedName

enum class SortDirection {
  @SerializedName("asc")
  ASC,

  @SerializedName("desc")
  DESC,
}
