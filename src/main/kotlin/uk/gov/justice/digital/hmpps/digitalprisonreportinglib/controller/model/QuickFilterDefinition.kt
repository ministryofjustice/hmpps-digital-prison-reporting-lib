package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

import com.fasterxml.jackson.annotation.JsonValue

enum class QuickFilterDefinition(@JsonValue val value: String) {
  TODAY("today"),
  YESTERDAY("yesterday"),
  LAST_SEVEN_DAYS("last-seven-days"),
  LAST_THIRTY_DAYS("last-thirty-days"),
  LAST_MONTH("last-month"),
  LAST_FULL_MONTH("last-full-month"),
  LAST_90_DAYS("last-90-days"),
  LAST_FULL_3_MONTHS("last-full-3-months"),
  LAST_YEAR("last-year"),
  LAST_FULL_YEAR("last-full-year"),
  TOMORROW("tomorrow"),
  NEXT_SEVEN_DAYS("next-seven-days"),
  NEXT_THIRTY_DAYS("next-thirty-days"),
  NEXT_MONTH("next-month"),
  NEXT_FULL_MONTH("next-full-month"),
  NEXT_90_DAYS("next-90-days"),
  NEXT_3_MONTHS("next-3-months"),
  NEXT_FULL_3_MONTHS("next-full-3-months"),
  NEXT_YEAR("next-year"),
  NEXT_FULL_YEAR("next-full-year"),
}
