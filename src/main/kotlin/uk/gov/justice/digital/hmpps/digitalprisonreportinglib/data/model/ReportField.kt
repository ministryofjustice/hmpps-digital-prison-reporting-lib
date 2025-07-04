package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.common.model.SortDirection

data class ReportField(
  val name: String,
  val display: String?,
  val wordWrap: WordWrap? = null,
  val filter: FilterDefinition? = null,
  val sortable: Boolean = true,
  @SerializedName("defaultsort")
  val defaultSort: Boolean = false,
  @SerializedName("sortdirection")
  val sortDirection: SortDirection? = null,
  val formula: String? = null,
  val visible: Visible? = null,
) : Identified() {
  override fun getIdentifier() = this.name
}
