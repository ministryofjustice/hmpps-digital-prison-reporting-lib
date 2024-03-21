package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

import com.fasterxml.jackson.annotation.JsonValue

enum class WordWrap(@JsonValue val value: String) {
  None("none"),
  Normal("normal"),
  BreakWords("break-words"),
}
