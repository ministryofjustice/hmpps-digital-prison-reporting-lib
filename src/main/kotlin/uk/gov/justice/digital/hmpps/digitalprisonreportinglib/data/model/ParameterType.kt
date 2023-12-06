package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.fasterxml.jackson.annotation.JsonValue

enum class ParameterType(@JsonValue val type: kotlin.String) {
  String("string"),
  Date("date"),
  Long("long"),
  Time("time"),
  ;
}
