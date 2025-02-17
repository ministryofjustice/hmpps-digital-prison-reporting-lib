package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

data class DashboardDefinition(
  val id: String,
  val name: String,
  val description: String,
  val sections: List<DashboardSectionDefinition>,
  val filterFields: List<FieldDefinition>?,
)
