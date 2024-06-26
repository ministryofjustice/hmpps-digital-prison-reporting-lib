package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName

data class FilterDefinition(
  val type: FilterType,
  val mandatory: Boolean = false,
  val pattern: String? = null,
  @SerializedName("staticoptions")
  val staticOptions: List<StaticFilterOption>? = null,
  @SerializedName("dynamicoptions")
  val dynamicOptions: DynamicFilterOption? = null,
  val default: String? = null,
  val min: String? = null,
  val max: String? = null,
)
