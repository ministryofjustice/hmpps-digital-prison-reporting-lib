package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

data class FieldDefinition(
  val name: String,
  val display: String,
  val wordWrap: WordWrap? = null,
  val filter: FilterDefinition? = null,
  val sortable: Boolean = true,
  val defaultsort: Boolean = false,
  val type: FieldType,
  val mandatory: Boolean? = false,
)
