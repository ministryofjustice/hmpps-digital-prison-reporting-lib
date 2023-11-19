package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

enum class FilterType(val type: String) {
  Radio("Radio"),
  Select("Select"),
  DATE_RANGE("date-range"),
}
