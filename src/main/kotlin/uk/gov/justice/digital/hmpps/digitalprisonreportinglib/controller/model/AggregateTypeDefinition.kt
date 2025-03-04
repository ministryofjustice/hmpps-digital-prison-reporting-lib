package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

import com.fasterxml.jackson.annotation.JsonValue

enum class AggregateTypeDefinition(@JsonValue val type: String) {
  SUM("sum"),
  AVERAGE("average"),
}
