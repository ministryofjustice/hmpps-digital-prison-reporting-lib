package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model

data class FieldDefinition(
  val name: String,
  val displayName: String,
  val wordWrap: WordWrap? = null,
  val filter: FilterDefinition? = null,
  val sortable: Boolean = true,
  val defaultSortColumn: Boolean = false,
  val type: FieldType,
)
