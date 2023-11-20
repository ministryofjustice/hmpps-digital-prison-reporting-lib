package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class ReportField(
  val name: String,
  val display: String,
  val wordWrap: WordWrap? = null,
  val filter: FilterDefinition? = null,
  val sortable: Boolean = true,
  val defaultsort: Boolean = false,
  val formula: String? = null,
  val visible: Boolean?,
)
