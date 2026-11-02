package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class FilterType {
  @SerialName("radio")
  Radio,

  @SerialName("select")
  Select,

  @SerialName("multiselect")
  Multiselect,

  @SerialName("caseloads")
  Caseloads,

  @SerialName("daterange")
  DateRange,

  @SerialName("autocomplete")
  AutoComplete,

  @SerialName("text")
  Text,

  @SerialName("date")
  Date,

  @SerialName("granulardaterange")
  Granulardaterange,

  @SerialName("autocompletemulti")
  AutoCompleteMulti,
}
