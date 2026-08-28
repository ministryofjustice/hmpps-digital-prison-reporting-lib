package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.common.model.SortDirection

@Serializable
data class ReportField(
  val name: String,
  val display: String?,
  val wordWrap: WordWrap? = null,
  val filter: FilterDefinition? = null,
  val sortable: Boolean = true,
  @SerialName("defaultsort")
  @SerializedName("defaultsort")
  val defaultSort: Boolean = false,
  @SerialName("sortdirection")
  @SerializedName("sortdirection")
  val sortDirection: SortDirection? = null,
  val formula: String? = null,
  val visible: Visible? = null,
) : Identified {
  override fun getIdentifier() = this.name
}
