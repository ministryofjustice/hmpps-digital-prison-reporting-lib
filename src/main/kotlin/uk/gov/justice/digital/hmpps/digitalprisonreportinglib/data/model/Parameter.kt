package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Parameter(
  val index: Int,
  val name: String,
  val reportFieldType: ParameterType,
  val filterType: FilterType,
  val display: String,
  val mandatory: Boolean,
  val referenceType: ReferenceType? = null,
  val default: String? = null,
)
