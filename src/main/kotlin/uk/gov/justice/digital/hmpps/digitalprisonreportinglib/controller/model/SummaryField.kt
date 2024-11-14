package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

data class SummaryField(
  val name: String,
  val display: String?,
  val type: FieldType?,
  val header: Boolean?,
  val mergeRows: Boolean?,
)
