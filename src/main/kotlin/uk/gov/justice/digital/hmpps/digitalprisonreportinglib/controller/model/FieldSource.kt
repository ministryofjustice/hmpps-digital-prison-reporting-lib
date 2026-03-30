package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

import com.fasterxml.jackson.annotation.JsonValue

enum class FieldSource(@JsonValue val type: kotlin.String) {
  SpecField("specfield"),
  ParamField("paramfield"),
}
