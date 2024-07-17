package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

data class ReportSummary(
  val id: String,
  val template: SummaryTemplate,
  val fields: List<SummaryField> = emptyList(),
)
