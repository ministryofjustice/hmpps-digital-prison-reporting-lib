package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName

data class FilterDefinition(
  val type: FilterType,
  @SerializedName("staticoptions")
  val staticOptions: List<FilterOption>? = null,
  val default: String? = null,
)
