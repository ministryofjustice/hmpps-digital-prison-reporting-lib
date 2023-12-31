package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.fasterxml.jackson.annotation.JsonValue

enum class FilterType(@JsonValue val type: String) {
  Radio("Radio"),
  DateRange("daterange"),
  AutoComplete("autocomplete"),
}
