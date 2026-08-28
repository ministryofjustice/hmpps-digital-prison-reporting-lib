package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName

enum class SummaryTemplate {
  @SerialName("table-header")
  @SerializedName("table-header")
  TableHeader,

  @SerialName("table-footer")
  @SerializedName("table-footer")
  TableFooter,

  @SerialName("section-header")
  @SerializedName("section-header")
  SectionHeader,

  @SerialName("section-footer")
  @SerializedName("section-footer")
  SectionFooter,

  @SerialName("page-header")
  @SerializedName("page-header")
  PageHeader,

  @SerialName("page-footer")
  @SerializedName("page-footer")
  PageFooter,
}
