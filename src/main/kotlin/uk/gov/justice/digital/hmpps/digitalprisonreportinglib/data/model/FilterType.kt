package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

enum class FilterType(val type: String) {
  Radio("Radio"),
  DateRange("daterange"),
  AutoComplete("autocomplete"),
}
