package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SchemaField(
  val name: String,
  val type: ParameterType,
  val display: String,
  val filter: FilterDefinition? = null,
  val formula: String? = null,
) : Identified {
  override fun getIdentifier() = this.name
}
