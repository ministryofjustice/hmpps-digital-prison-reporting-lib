package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Visible

data class FieldDefinition(
  val name: String,
  val display: String,
  val wordWrap: WordWrap? = null,
  val filter: FilterDefinition? = null,
  val sortable: Boolean = true,
  val defaultsort: Boolean = false,
  val type: FieldType,
  val mandatory: Boolean? = false,
  val visible: Visible? = Visible.TRUE,
)
