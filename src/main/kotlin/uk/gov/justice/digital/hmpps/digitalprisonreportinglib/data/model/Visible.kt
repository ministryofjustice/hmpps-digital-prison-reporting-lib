package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.fasterxml.jackson.annotation.JsonValue

enum class Visible(@JsonValue val value: String) {
  TRUE("true"),
  FALSE("false"),
  MANDATORY("mandatory"),
}
