package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

import com.fasterxml.jackson.annotation.JsonValue

enum class FieldType(@JsonValue val type: kotlin.String) {
  Boolean("boolean"),
  Date("date"),
  Double("double"),
  HTML("HTML"),
  Long("long"),
  String("string"),
  Time("time"),
}
