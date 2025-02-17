package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class SchemaField(
  val name: String,
  val type: ParameterType,
  val display: String,
  val filter: FilterDefinition? = null,
) : Identified() {
  override fun getIdentifier() = this.name
}
