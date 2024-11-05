package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class ReportSummary(
  val id: String,
  val dataset: String,
  val template: SummaryTemplate,
  val field: List<SummaryField>?,
)
