package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model

data class Specification(
  val template: String,
  val fields: List<FieldDefinition>,
)
