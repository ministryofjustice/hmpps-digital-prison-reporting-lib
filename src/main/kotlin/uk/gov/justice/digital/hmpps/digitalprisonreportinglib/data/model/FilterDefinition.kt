package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FilterDefinition(
  val type: FilterType,
  val mandatory: Boolean = false,
  val pattern: String? = null,
  @SerialName("staticoptions")
  @SerializedName("staticoptions")
  val staticOptions: List<StaticFilterOption>? = null,
  @SerialName("dynamicoptions")
  @SerializedName("dynamicoptions")
  val dynamicOptions: DynamicFilterOption? = null,
  val default: String? = null,
  val min: String? = null,
  val max: String? = null,
  val interactive: Boolean? = false,
  val defaultGranularity: Granularity? = null,
  val defaultQuickFilterValue: QuickFilter? = null,
  val index: Int? = null,
  val minSelected: Int? = null,
  val maxSelected: Int? = null,
)
