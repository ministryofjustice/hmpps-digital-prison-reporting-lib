package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

import com.fasterxml.jackson.annotation.JsonValue

enum class GranularityDefinition(@JsonValue val value: String) {
  HOURLY("hourly"),
  DAILY("daily"),
  WEEKLY("weekly"),
  MONTHLY("monthly"),
  QUARTERLY("quarterly"),
  ANNUALLY("annually"),
}
