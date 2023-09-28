package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model

data class ReportField(
  val schemaField: String,
  val displayName: String,
  val wordWrap: WordWrap? = null,
  val filter: FilterDefinition? = null,
  val sortable: Boolean = true,
  val defaultSortColumn: Boolean = false,
)
