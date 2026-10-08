package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SummaryField(
  val name: String,
  val header: Boolean? = false,
  val mergeRows: Boolean? = false,
  @SerialName("defaultsort")
  @SerializedName("defaultsort")
  val defaultSort: Boolean = false,
) : Identified {
  override fun getIdentifier() = this.name
}
