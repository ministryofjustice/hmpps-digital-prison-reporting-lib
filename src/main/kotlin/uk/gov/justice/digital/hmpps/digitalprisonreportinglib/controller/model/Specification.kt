package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

data class Specification(
  val template: Template,
  val fields: List<FieldDefinition>,
  val sections: List<String> = emptyList(),
)
