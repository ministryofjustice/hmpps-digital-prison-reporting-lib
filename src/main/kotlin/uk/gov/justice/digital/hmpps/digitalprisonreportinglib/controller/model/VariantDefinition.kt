package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

data class VariantDefinition(
  val id: String,
  val name: String,
  val resourceName: String,
  val description: String? = null,
  val specification: Specification? = null,
)
