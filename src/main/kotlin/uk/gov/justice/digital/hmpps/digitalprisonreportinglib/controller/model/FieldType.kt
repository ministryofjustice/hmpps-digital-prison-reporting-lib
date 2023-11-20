package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

import com.fasterxml.jackson.annotation.JsonValue

enum class FieldType(@JsonValue val type: kotlin.String) {
  String("string"),
  Date("date"),
  Long("long"),
  ;
}
