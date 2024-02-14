package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName

data class ReportField(
  val name: String,
  val display: String,
  val wordWrap: WordWrap? = null,
  val filter: FilterDefinition? = null,
  val sortable: Boolean = true,
  @SerializedName("defaultsort")
  val defaultSort: Boolean = false,
  // Formula and visible are not used yet. This is pending ticket https://dsdmoj.atlassian.net/browse/DPR2-241
  val formula: String? = null,
  val visible: Boolean?,
  val mandatory: Boolean? = false,
)
