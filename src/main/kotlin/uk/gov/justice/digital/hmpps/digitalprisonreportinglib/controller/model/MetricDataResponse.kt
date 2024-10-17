package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

import java.time.LocalDateTime

data class MetricDataResponse(
  val id: String,
  val data: List<Map<String, Any?>>,
  val updated: LocalDateTime,
)
