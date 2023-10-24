package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class SchemaField(
  val name: String,
  val type: ParameterType,
  val caseload: Boolean = false
)
