package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

import com.fasterxml.jackson.annotation.JsonValue

enum class SortDirection(@JsonValue val value: String) {
  ASC("asc"),
  DESC("desc"),
}
