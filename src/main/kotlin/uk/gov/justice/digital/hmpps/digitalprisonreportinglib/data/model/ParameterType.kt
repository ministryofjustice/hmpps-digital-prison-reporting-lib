package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

enum class ParameterType(val type: kotlin.String) {
  String("string"),
  Date("date"),
  Long("long"),
  ;
}
