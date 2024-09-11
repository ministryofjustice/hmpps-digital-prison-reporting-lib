package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName

enum class SummaryTemplate {
  @SerializedName("table-header")
  TableHeader,

  @SerializedName("table-footer")
  TableFooter,

  @SerializedName("section-header")
  SectionHeader,

  @SerializedName("section-footer")
  SectionFooter,

  @SerializedName("page-header")
  PageHeader,

  @SerializedName("page-footer")
  PageFooter,
}
