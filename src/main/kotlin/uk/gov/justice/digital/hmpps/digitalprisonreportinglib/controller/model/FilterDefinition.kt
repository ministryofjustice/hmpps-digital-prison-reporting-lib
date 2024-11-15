package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

import com.google.gson.annotations.SerializedName
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.DynamicFilterOption

data class FilterDefinition(
  val type: FilterType,
  val mandatory: Boolean = false,
  val pattern: String? = null,
  val staticOptions: List<FilterOption>? = null,
  @SerializedName("dynamicoptions")
  val dynamicOptions: DynamicFilterOption? = null,
  val defaultValue: String? = null,
  val min: String? = null,
  val max: String? = null,
  val interactive: Boolean? = false,
)
