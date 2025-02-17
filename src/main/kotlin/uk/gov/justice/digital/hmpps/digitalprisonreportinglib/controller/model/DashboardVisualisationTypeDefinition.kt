package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

import com.fasterxml.jackson.annotation.JsonValue

enum class DashboardVisualisationTypeDefinition(@JsonValue val type: String) {
  LIST("list"),

  DOUGHNUT("doughnut"),

  BAR("bar"),

  BAR_TIMESERIES("bar-timeseries"),

  LINE("line"),

  SCORECARD("scorecard"),

  SCORECARD_GROUP("scorecard-group"),
}
