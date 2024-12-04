package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class Parameter(
  val index: Int,
  val name: String,
  val reportFieldType: ParameterType,
  val filterType: FilterType,
  val display: String,
  val mandatory: Boolean,
  val specialType: SpecialType? = null,
)
