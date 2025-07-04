package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.common.model.SortDirection

data class FieldDefinition(
  val name: String,
  val display: String,
  val wordWrap: WordWrap? = null,
  val filter: FilterDefinition? = null,
  val sortable: Boolean = true,
  val defaultsort: Boolean = false,
  val sortDirection: SortDirection? = null,
  val type: FieldType,
  val mandatory: Boolean = false,
  val visible: Boolean = true,
  val calculated: Boolean = false,
  val header: Boolean = false,
)
