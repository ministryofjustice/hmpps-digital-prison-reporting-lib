package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

data class FilterDefinition(
  val type: FilterType,
  val staticOptions: List<FilterOption>? = null,
  val defaultValue: String?,
)
