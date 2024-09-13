package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

import com.fasterxml.jackson.annotation.JsonValue

enum class DashboardChartTypeDefinition(@JsonValue val type: String) {
  DOUGHNUT("doughnut"),
  BAR("bar"),
  LINE("line"),
}