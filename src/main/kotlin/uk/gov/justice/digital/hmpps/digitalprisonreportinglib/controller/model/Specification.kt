package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

data class Specification(
  val template: String,
  val fields: List<FieldDefinition>,
)
