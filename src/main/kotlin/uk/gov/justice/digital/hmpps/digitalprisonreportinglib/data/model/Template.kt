package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Template {
  @SerializedName("list")
  List,

  @SerialName("list-section")
  @SerializedName("list-section")
  ListSection,

  @SerialName("list-tab")
  @SerializedName("list-tab")
  ListTab,

  @SerializedName("summary")
  Summary,

  @SerialName("summary-section")
  @SerializedName("summary-section")
  SectionedSummary,

  @SerialName("parent-child")
  @SerializedName("parent-child")
  ParentChild,

  @SerialName("parent-child-section")
  @SerializedName("parent-child-section")
  ParentChildSection,

  @SerialName("row-section")
  @SerializedName("row-section")
  RowSection,

  @SerialName("row-section-child")
  @SerializedName("row-section-child")
  RowSectionChild,
}
