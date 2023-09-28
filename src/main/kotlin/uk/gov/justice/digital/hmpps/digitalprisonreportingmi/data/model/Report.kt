package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model

import java.time.LocalDate

data class Report(
  val id: String,
  val name: String,
  val description: String? = null,
  val created: LocalDate,
  val version: String,
  val dataset: String,
  val policy: List<String> = emptyList(),
  val render: RenderMethod,
  val schedule: String? = null,
  val specification: Specification? = null,
  val destination: List<Map<String, String>> = emptyList(),
)
