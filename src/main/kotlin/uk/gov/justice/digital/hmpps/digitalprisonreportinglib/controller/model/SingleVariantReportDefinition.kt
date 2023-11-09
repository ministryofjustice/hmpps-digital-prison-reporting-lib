package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

class SingleVariantReportDefinition(
  val id: String,
  val name: String,
  val description: String? = null,
  val variant: VariantDefinition,
)
