package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName

enum class Template {
  @SerializedName("list")
  List,

  @SerializedName("list-section")
  ListSection,

  @SerializedName("list-aggregate")
  ListAggregate,

  @SerializedName("list-tab")
  ListTab,

  @SerializedName("crosstab")
  CrossTab,

  @SerializedName("summary")
  Summary,

  @SerializedName("summary-section")
  SectionedSummary,
}
