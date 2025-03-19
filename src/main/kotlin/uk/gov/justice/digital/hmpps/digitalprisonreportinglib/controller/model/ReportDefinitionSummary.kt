package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

data class ReportDefinitionSummary(
  val id: String,
  val name: String,
  val description: String? = null,
  val variants: List<VariantDefinitionSummary>,
  val dashboards: List<DashboardDefinitionSummary>? = null,
  val authorised: Boolean,
)
