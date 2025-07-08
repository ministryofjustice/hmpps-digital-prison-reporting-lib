package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

data class VariantDefinitionSummary(
  val id: String,
  val name: String,
  val description: String? = null,
  val isMissing: Boolean = false,
)
