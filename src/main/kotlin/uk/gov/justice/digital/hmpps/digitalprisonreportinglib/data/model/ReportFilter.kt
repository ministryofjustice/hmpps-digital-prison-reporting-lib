package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class ReportFilter(
  val name: String,
  val query: String,
  val id: String? = null,
  val description: String? = null,
  val database: String? = null,
  val version: String? = null,
)
