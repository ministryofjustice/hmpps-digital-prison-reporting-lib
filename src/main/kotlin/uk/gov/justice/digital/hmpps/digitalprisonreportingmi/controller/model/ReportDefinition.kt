package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model

data class ReportDefinition(
  val id: String,
  val name: String,
  val description: String? = null,
  val variants: List<VariantDefinition>,
)
