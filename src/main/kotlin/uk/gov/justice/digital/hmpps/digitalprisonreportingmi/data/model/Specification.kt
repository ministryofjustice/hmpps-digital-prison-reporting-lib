package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model

data class Specification(
  val template: String,
  val field: List<ReportField>,
)
