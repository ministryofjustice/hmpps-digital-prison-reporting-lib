package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

import com.fasterxml.jackson.annotation.JsonValue

enum class FilterType(@JsonValue val type: String) {
  Radio("Radio"),
  Select("Select"),
  Multiselect("multiselect"),
  DateRange("daterange"),
  AutoComplete("autocomplete"),
  Text("text"),
  Date("date"),
  GranularDateRange("granulardaterange"),
}
